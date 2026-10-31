package com.tiendatecnologica.repository;
import java.util.List;

//su extension cambia a la de support ya que la relacion entre proveedor y producto no es un modelo propio del sistema
//ni tiene una clave de texto propia (su clave primaria es compuesta: codigo_producto + id_proveedor), es solo una clase que sirve para hacer la relacion de N:M
//se extiende de support por esto mismo , ya  que solo se van a necesitar las cunsultas a la tabla y metodos de ejecucion
//con jdbc repository si hay una logica de como se administran , guardan y leen los datos de sus respectivas tablas
//en este no, solo se necesitan hacer consultas o modificaciones pero con datos de productos o proveedores nunca datos propios


//basicamente sirve para listas de parejas entre provedores y productos
public class ProductoProveedorRepository extends JdbcSupport {

    // Crea el vínculo entre un producto y un proveedor (una fila nueva en producto_proveedor).
    // Si el par ya existe, MySQL lanza el error 1062 y RepositorioException lo traduce.
    // Si el producto o el proveedor no existen, lanza el 1452 (clave foránea).
    public void asociar(String codigoProducto, String idProveedor) {
        ejecutar("INSERT INTO producto_proveedor (codigo_producto, id_proveedor) "
                        + "VALUES (?, ?)",
                "asociar el producto con el proveedor",
                codigoProducto, idProveedor);
    }

    // Quita el vínculo entre un producto y un proveedor.
    // El WHERE usa las DOS columnas; si usara solo una, borraría todas las
    // asociaciones de ese producto (o de ese proveedor).
    // Devuelve true si realmente borró una fila, false si el par no existía.
    public boolean desasociar(String codigoProducto, String idProveedor) {
        int filas = ejecutar("DELETE FROM producto_proveedor "
                        + "WHERE codigo_producto = ? AND id_proveedor = ?",
                "quitar la asociación entre el producto y el proveedor",
                codigoProducto, idProveedor);
        return filas > 0;
    }

    // Indica si el producto y el proveedor ya están asociados.
    // Es el mismo patrón de existe() en JdbcRepository: un SELECT 1 y
    // se revisa si la lista que devuelve consultar() trae algo.
    public boolean existeAsociacion(String codigoProducto, String idProveedor) {
        List<Integer> resultado = consultar(
                "SELECT 1 FROM producto_proveedor "
                        + "WHERE codigo_producto = ? AND id_proveedor = ?",
                "verificar si el producto y el proveedor están asociados",
                rs -> 1, codigoProducto, idProveedor);
        return !resultado.isEmpty();
    }
}