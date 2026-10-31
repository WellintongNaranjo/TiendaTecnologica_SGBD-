package com.tiendatecnologica.repository;

import com.tiendatecnologica.model.Producto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
public class ProductoRepository extends JdbcRepository<Producto> {

    // Aquí va toda la lógica de cómo se envían los datos de Java a la BD (el DAO).

    // Nombre de la tabla en MySQL.
    @Override
    protected String tabla() {
        return "producto";
    }

    // Columna que identifica de forma única a cada producto.
    @Override
    protected String columnaClave() {
        return "codigo";
    }

    // Palabra para los mensajes de error: "No se pudo guardar el producto".
    @Override
    protected String entidad() {
        return "producto";
    }

    // Las 9 columnas de la tabla, en orden, y 9 signos ? (uno por columna).
    @Override
    protected String sqlInsertar() {
        return "INSERT INTO producto (codigo, nombre, categoria, marca, "
                + "precio_compra, precio_venta, cantidad_disponible, "
                + "stock_minimo, meses_garantia) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    // El UPDATE no modifica codigo (la clave solo se usa en el WHERE) ni
    // cantidad_disponible: el stock solo cambia por movimientos de inventario
    // (ventas, compras, devoluciones, ajustes), así editar un producto no lo pisa.
    @Override
    protected String sqlActualizar() {
        return "UPDATE producto SET nombre = ?, categoria = ?, marca = ?, "
                + "precio_compra = ?, precio_venta = ?, "
                + "stock_minimo = ?, meses_garantia = ? "
                + "WHERE codigo = ?";
    }

    // Valores del INSERT, en el mismo orden que las 9 columnas.
    // Se usa Object[] porque mezcla tipos distintos (texto, decimales, enteros).
    //sirve para sacar los valores del producto
    @Override
    protected Object[] parametrosInsertar(Producto p) {
        return new Object[] {
                p.getCodigo(), p.getNombre(), p.getCategoria(), p.getMarca(),
                p.getPrecioCompra(), p.getPrecioVenta(), p.getCantidadDisponible(),
                p.getStockMinimo(), p.getMesesGarantia()
        };
    }

    // Valores del UPDATE (8 ? = 7 columnas + el WHERE); el código va al final.
    // Se usa Object[] porque mezcla tipos distintos (texto, decimales, enteros).
    @Override
    protected Object[] parametrosActualizar(Producto p) {
        return new Object[] {
                p.getNombre(), p.getCategoria(), p.getMarca(),
                p.getPrecioCompra(), p.getPrecioVenta(),
                p.getStockMinimo(), p.getMesesGarantia(), p.getCodigo()
        };
    }

    // Recibe una fila de MySQL y reconstruye el objeto Producto.
    @Override
    protected Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                rs.getString("marca"),
                rs.getDouble("precio_compra"),
                rs.getDouble("precio_venta"),
                rs.getInt("cantidad_disponible"),
                rs.getInt("stock_minimo"),
                rs.getInt("meses_garantia"));
    }



    //-------------------------------------------------------------------------------------
    // JOIN PRODUCTO-PROVEEDOR: consulta de la relación N:M, exclusiva para este caso.
    // Recibe el id de un proveedor y retorna los productos que surte.
    // Va en ProductoRepository porque devuelve objetos Producto y aquí ya existe
    // mapear(), así no se duplica cómo construir un Producto.
    //-------------------------------------------------------------------------------------


    // Devuelve los productos que surte un proveedor.
    // Es simétrico al de proveedor: cruza producto con producto_proveedor,
    // donde pp.codigo_producto coincide con p.codigo, y se filtra por el id del proveedor.
    // Si el proveedor no surte productos, devuelve una lista vacía (no null).
    public List<Producto> obtenerPorProveedor(String idProveedor) {
        return consultar(
                "SELECT p.* FROM producto p "
                        + "JOIN producto_proveedor pp ON pp.codigo_producto = p.codigo "
                        + "WHERE pp.id_proveedor = ? "
                        + "ORDER BY p.codigo",
                "consultar los productos del proveedor",
                this::mapear, idProveedor);
    }
}