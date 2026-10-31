package com.tiendatecnologica.repository;

import com.tiendatecnologica.enums.EstadoVenta;
import com.tiendatecnologica.enums.MetodoPago;
import com.tiendatecnologica.model.DetalleCompra;
import com.tiendatecnologica.model.OrdenCompra;
import com.tiendatecnologica.model.Pago;
import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.ProductoVendido;
import com.tiendatecnologica.model.Venta;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consultas de solo lectura para los reportes. El agrupado, el filtrado y las
 * sumas los hace MySQL (JOIN, GROUP BY, SUM), en vez de recorrer listas en Java
 * y consultar producto por producto.
 *
 * Reutiliza las herramientas de {@link JdbcSupport} (PreparedStatement,
 * cierre de conexiones y traducción de errores). Las filas se convierten en
 * objetos del modelo antes de salir de esta clase: nunca se devuelve un
 * ResultSet ni una conexión abierta.
 */
public class ReporteRepository extends JdbcSupport {

    /** Límite inferior usado cuando no se indica fecha "Desde" (así el SQL siempre lleva el mismo filtro). */
    private static final LocalDate SIN_LIMITE_INFERIOR = LocalDate.of(1900, 1, 1);

    /** Límite superior (excluido) usado cuando no se indica fecha "Hasta". */
    private static final LocalDate SIN_LIMITE_SUPERIOR = LocalDate.of(9999, 1, 1);

    /** Línea de una orden de compra leída con LEFT JOIN (el detalle es null si la orden no tiene líneas). */
    private record FilaCompra(String idOrden, String idProveedor, String fecha,
                              String estado, DetalleCompra detalle) {
    }

    /**
     * Obtiene las ventas (completadas y anuladas) hechas dentro de un rango de fechas,
     * de la más antigua a la más reciente. El filtro lo aplica MySQL.
     *
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido (hasta las 23:59:59), o null para no limitar por el final
     * @return lista de ventas con su pago reconstruido
     */
    public List<Venta> obtenerVentas(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT id, fecha, id_cliente, subtotal, impuesto, total, "
                + "metodo_pago, monto_recibido, vuelto, estado "
                + "FROM venta WHERE fecha >= ? AND fecha < ? ORDER BY fecha, id";
        return consultar(sql, "consultar el reporte de ventas", rs -> {
            Pago pago = Pago.reconstruir(
                    MetodoPago.valueOf(rs.getString("metodo_pago")),
                    rs.getDouble("monto_recibido"),
                    rs.getDouble("vuelto"));
            return new Venta(
                    rs.getString("id"),
                    rs.getObject("fecha", LocalDateTime.class),
                    rs.getString("id_cliente"),          // puede ser null (venta sin cliente)
                    rs.getDouble("subtotal"),
                    rs.getDouble("impuesto"),
                    rs.getDouble("total"),
                    pago,
                    EstadoVenta.valueOf(rs.getString("estado")));
        }, inicioDelDia(desde), finDelRango(hasta));
    }

    /**
     * Suma las unidades vendidas por producto, contando solo ventas COMPLETADAS
     * (las anuladas no suman) hechas dentro del rango de fechas, de mayor a menor
     * cantidad. El filtro lo aplica MySQL.
     *
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return una fila por producto vendido al menos una vez en ese rango
     */
    public List<ProductoVendido> obtenerProductosMasVendidos(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT p.codigo, p.nombre, SUM(dv.cantidad) AS unidades "
                + "FROM detalle_venta dv "
                + "JOIN venta v ON v.id = dv.id_venta "
                + "JOIN producto p ON p.codigo = dv.codigo_producto "
                + "WHERE v.estado = ? AND v.fecha >= ? AND v.fecha < ? "
                + "GROUP BY p.codigo, p.nombre "
                + "ORDER BY unidades DESC, p.codigo";
        return consultar(sql, "consultar los productos más vendidos",
                rs -> new ProductoVendido(rs.getString("codigo"), rs.getString("nombre"), rs.getInt("unidades")),
                EstadoVenta.COMPLETADA.name(), inicioDelDia(desde), finDelRango(hasta));
    }

    /**
     * Obtiene los productos con stock en o por debajo del mínimo. Los agotados
     * (cantidad 0) ya quedan incluidos y cada producto aparece una sola vez.
     *
     * @return productos a reabastecer, los de menor existencia primero
     */
    public List<Producto> obtenerProductosBajoInventario() {
        String sql = "SELECT codigo, nombre, categoria, marca, precio_compra, precio_venta, "
                + "cantidad_disponible, stock_minimo, meses_garantia "
                + "FROM producto "
                + "WHERE cantidad_disponible <= stock_minimo OR cantidad_disponible = 0 "
                + "ORDER BY cantidad_disponible, codigo";
        return consultar(sql, "consultar los productos con bajo inventario", rs -> new Producto(
                rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("categoria"), rs.getString("marca"),
                rs.getDouble("precio_compra"), rs.getDouble("precio_venta"),
                rs.getInt("cantidad_disponible"), rs.getInt("stock_minimo"),
                rs.getInt("meses_garantia")));
    }

    /**
     * Obtiene las órdenes de compra con sus detalles en una sola consulta (LEFT JOIN),
     * así {@code OrdenCompra.calcularTotal()} funciona sin consultar cada orden por
     * separado. Solo incluye las órdenes con fecha dentro del rango indicado (la fecha
     * de una orden es un día, sin hora). El filtro lo aplica MySQL.
     *
     * El estado se devuelve con el formato que ya usa la aplicación ("Pendiente",
     * "Recibida", "Cancelada"), igual que {@code OrdenCompraRepository}, aunque en
     * MySQL esté en mayúsculas.
     *
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return órdenes de ese rango ordenadas por id, cada una con su lista de detalles
     */
    public List<OrdenCompra> obtenerCompras(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT oc.id_orden, oc.id_proveedor, oc.fecha, oc.estado, "
                + "dc.codigo_producto, dc.cantidad, dc.costo_unitario "
                + "FROM orden_compra oc "
                + "LEFT JOIN detalle_compra dc ON dc.id_orden = oc.id_orden "
                + "WHERE oc.fecha >= ? AND oc.fecha < ? "
                + "ORDER BY oc.id_orden, dc.id";
        List<FilaCompra> filas = consultar(sql, "consultar el reporte de compras", rs -> {
            String codigo = rs.getString("codigo_producto");
            DetalleCompra detalle = codigo == null ? null
                    : new DetalleCompra(codigo, rs.getInt("cantidad"), rs.getDouble("costo_unitario"));
            return new FilaCompra(rs.getString("id_orden"), rs.getString("id_proveedor"),
                    rs.getString("fecha"), rs.getString("estado"), detalle);
        }, desde == null ? SIN_LIMITE_INFERIOR : desde, hasta == null ? SIN_LIMITE_SUPERIOR : hasta.plusDays(1));

        Map<String, OrdenCompra> ordenes = new LinkedHashMap<>();
        for (FilaCompra fila : filas) {
            OrdenCompra orden = ordenes.computeIfAbsent(fila.idOrden(), id -> new OrdenCompra(
                    id, fila.idProveedor(), fila.fecha(), capitalizar(fila.estado()), new ArrayList<>()));
            if (fila.detalle() != null) {
                orden.getDetalles().add(fila.detalle());
            }
        }
        return new ArrayList<>(ordenes.values());
    }

    /**
     * Calcula la ganancia ESTIMADA de las ventas COMPLETADAS dentro del rango de fechas:
     * precio al que se vendió cada línea (guardado en {@code detalle_venta}) menos el
     * {@code precio_compra} ACTUAL del producto. Es una estimación porque la base de
     * datos no guarda el costo que tenía el producto al momento de la venta.
     *
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return ganancia estimada de ese rango, redondeada a 2 decimales (0 si no hay ventas)
     */
    public double calcularGananciaEstimada(LocalDate desde, LocalDate hasta) {
        String sql = "SELECT COALESCE(SUM((dv.precio_unitario - p.precio_compra) * dv.cantidad), 0) AS ganancia "
                + "FROM detalle_venta dv "
                + "JOIN venta v ON v.id = dv.id_venta "
                + "JOIN producto p ON p.codigo = dv.codigo_producto "
                + "WHERE v.estado = ? AND v.fecha >= ? AND v.fecha < ?";
        List<Double> resultado = consultar(sql, "calcular la ganancia estimada",
                rs -> rs.getDouble("ganancia"),
                EstadoVenta.COMPLETADA.name(), inicioDelDia(desde), finDelRango(hasta));
        double ganancia = resultado.isEmpty() ? 0 : resultado.get(0);
        return Math.round(ganancia * 100.0) / 100.0;
    }

    /** @return las 00:00 del día "desde", o un límite muy antiguo si es null */
    private static LocalDateTime inicioDelDia(LocalDate desde) {
        return (desde == null ? SIN_LIMITE_INFERIOR : desde).atStartOfDay();
    }

    /** @return las 00:00 del día siguiente a "hasta" (límite excluido), o un límite muy lejano si es null */
    private static LocalDateTime finDelRango(LocalDate hasta) {
        return (hasta == null ? SIN_LIMITE_SUPERIOR : hasta.plusDays(1)).atStartOfDay();
    }

    /** Convierte "PENDIENTE" en "Pendiente" (formato que usa CompraService). */
    private static String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) {
            return texto;
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }
}
