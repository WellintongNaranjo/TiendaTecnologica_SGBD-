package com.tiendatecnologica.service;

import com.tiendatecnologica.enums.EstadoVenta;
import com.tiendatecnologica.enums.MetodoPago;
import com.tiendatecnologica.enums.TipoMovimiento;
import com.tiendatecnologica.model.Devolucion;
import com.tiendatecnologica.model.DetalleVenta;
import com.tiendatecnologica.model.MovimientoInventario;
import com.tiendatecnologica.model.Pago;
import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.Venta;
import com.tiendatecnologica.persistence.Transaccion;
import com.tiendatecnologica.repository.ClienteRepository;
import com.tiendatecnologica.repository.DetalleVentaRepository;
import com.tiendatecnologica.repository.DevolucionRepository;
import com.tiendatecnologica.repository.VentaRepository;
import com.tiendatecnologica.util.Constantes;
import com.tiendatecnologica.util.GeneradorId;
import com.tiendatecnologica.util.Validador;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class VentaService {

    private final VentaRepository ventaRepository = new VentaRepository();
    private final DetalleVentaRepository detalleVentaRepository = new DetalleVentaRepository();
    private final ClienteRepository clienteRepository = new ClienteRepository();
    private final DevolucionRepository devolucionRepository = new DevolucionRepository();

    private final ProductoService productoService = new ProductoService();
    private final InventarioService inventarioService = new InventarioService();



    public DetalleVenta crearItemCarrito(String codigoProducto, int cantidad) {
        Validador.validarTexto(codigoProducto, "Código de producto");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
        }

        Producto producto = productoService.buscarProducto(codigoProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe el producto " + codigoProducto);
        }
        if (!inventarioService.hayStock(codigoProducto, cantidad)) {
            throw new IllegalStateException(
                    "Stock insuficiente de " + producto.getNombre()
                            + " (disponible: " + producto.getCantidadDisponible() + ")");
        }

        DetalleVenta item = new DetalleVenta(null, codigoProducto, cantidad, producto.getPrecioVenta());
        item.setNombreProducto(producto.getNombre());
        return item;
    }



    public Venta registrarVenta(String idCliente, List<DetalleVenta> items,
                                MetodoPago metodoPago, double montoRecibido) {
        Validador.validarTexto(idCliente, "Cliente");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El carrito no puede estar vacío");
        }
        if (!clienteRepository.existe(idCliente)) {
            throw new IllegalArgumentException("No existe el cliente " + idCliente);
        }



        Map<String, Integer> cantidadPorProducto = new LinkedHashMap<>();
        for (DetalleVenta item : items) {
            if (item.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor que 0");
            }
            cantidadPorProducto.merge(item.getCodigoProducto(), item.getCantidad(), Integer::sum);
        }
        for (Map.Entry<String, Integer> entrada : cantidadPorProducto.entrySet()) {
            if (!inventarioService.hayStock(entrada.getKey(), entrada.getValue())) {
                throw new IllegalStateException("Stock insuficiente del producto " + entrada.getKey());
            }
        }

        double subtotal = 0;
        for (DetalleVenta item : items) {
            subtotal += item.getSubtotal();
        }
        subtotal = redondear(subtotal);
        double impuesto = redondear(subtotal * Constantes.IMPUESTO);
        double total = redondear(subtotal + impuesto);



        Pago pago = new Pago(metodoPago, montoRecibido, total);

        final double subtotalVenta = subtotal;



        Venta venta = Transaccion.ejecutar(con -> {
            String idVenta = GeneradorId.generarIdVenta(con);
            Venta nueva = new Venta(idVenta, LocalDateTime.now(), idCliente,
                    subtotalVenta, impuesto, total, pago, EstadoVenta.COMPLETADA);
            ventaRepository.guardar(con, nueva);

            for (DetalleVenta item : items) {
                DetalleVenta detalle = new DetalleVenta(
                        idVenta, item.getCodigoProducto(), item.getCantidad(), item.getPrecioUnitario());
                detalleVentaRepository.guardar(con, detalle);
                inventarioService.registrarSalida(
                        con, item.getCodigoProducto(), item.getCantidad(), "Venta " + idVenta);
            }
            return nueva;
        });

        return venta;
    }


    /**
     * Anula una venta y devuelve al inventario las unidades que todavía no
     * regresaron. Si alguna devolución de esta venta ya fue aprobada, su
     * cantidad ya se reintegró al aprobarla, así que se descuenta aquí para
     * no reintegrar dos veces el mismo stock. La anulación completa (entradas
     * de inventario y cambio de estado) se guarda en una sola transacción.
     *
     * @param idVenta id de la venta a anular
     * @throws IllegalArgumentException si la venta no existe
     * @throws IllegalStateException si la venta ya está anulada
     */
    public void anularVenta(String idVenta) {
        Venta venta = ventaRepository.buscarPorClave(idVenta);
        if (venta == null) {
            throw new IllegalArgumentException("No existe la venta " + idVenta);
        }
        if (!venta.estaCompletada()) {
            throw new IllegalStateException("La venta " + idVenta + " ya está anulada");
        }

        // Unidades vendidas por producto (una venta puede repetir un producto en varias líneas).
        Map<String, Integer> porReintegrar = new LinkedHashMap<>();
        for (DetalleVenta detalle : detalleVentaRepository.obtenerPorVenta(idVenta)) {
            porReintegrar.merge(detalle.getCodigoProducto(), detalle.getCantidad(), Integer::sum);
        }

        // Se resta lo que ya volvió al inventario por devoluciones aprobadas.
        for (Devolucion devolucion : devolucionRepository.obtenerPorVenta(idVenta)) {
            if (devolucionReintegroStock(devolucion)) {
                porReintegrar.computeIfPresent(devolucion.getCodigoProducto(),
                        (codigo, cantidad) -> Math.max(0, cantidad - devolucion.getCantidad()));
            }
        }

        Transaccion.ejecutar(con -> {
            for (Map.Entry<String, Integer> linea : porReintegrar.entrySet()) {
                if (linea.getValue() > 0) {
                    inventarioService.registrarEntrada(
                            con, linea.getKey(), linea.getValue(), "Anulación venta " + idVenta);
                }
            }
            venta.setEstado(EstadoVenta.ANULADA);
            ventaRepository.actualizar(con, venta);
            return null;
        });
    }

    /**
     * Indica si una devolución ya devolvió su cantidad al inventario. Una
     * devolución FINALIZADA pudo venir de APROBADA o de RECHAZADA, por eso se
     * confirma buscando su movimiento de entrada ("Devolución ID") en el
     * historial del producto.
     *
     * @param devolucion devolución a revisar
     * @return true si existe la entrada de inventario generada al aprobarla
     */
    private boolean devolucionReintegroStock(Devolucion devolucion) {
        String motivo = "Devolución " + devolucion.getId();
        for (MovimientoInventario m
                : inventarioService.obtenerMovimientosProducto(devolucion.getCodigoProducto())) {
            if (m.getTipo() == TipoMovimiento.ENTRADA && motivo.equals(m.getMotivo())) {
                return true;
            }
        }
        return false;
    }

    public Venta buscarVenta(String idVenta) {
        return ventaRepository.buscarPorClave(idVenta);
    }



    public List<Venta> listarVentas() {
        return ventaRepository.obtenerTodos();
    }




    public List<DetalleVenta> obtenerDetalle(String idVenta) {
        List<DetalleVenta> detalles = detalleVentaRepository.obtenerPorVenta(idVenta);
        for (DetalleVenta detalle : detalles) {
            Producto producto = productoService.buscarProducto(detalle.getCodigoProducto());
            if (producto != null) {
                detalle.setNombreProducto(producto.getNombre());
            }
        }
        return detalles;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
