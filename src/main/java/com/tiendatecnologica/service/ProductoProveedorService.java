package com.tiendatecnologica.service;

import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.Proveedor;
import com.tiendatecnologica.repository.ProductoRepository;
import com.tiendatecnologica.repository.ProductoProveedorRepository;
import com.tiendatecnologica.repository.ProveedorRepository;
import com.tiendatecnologica.util.Validador;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lógica de negocio de la relación N:M entre productos y proveedores.
 *
 * Esta relación es nueva en el P2: en el P1 un producto no sabía quién lo
 * surtía. Aquí se validan los datos y se evita asociar dos veces el mismo
 * par antes de llegar a la base de datos.
 */
public class ProductoProveedorService {

    // Maneja la tabla de parejas producto_proveedor (asociar, desasociar, existe).
    private final ProductoProveedorRepository asociacionRepository =
            new ProductoProveedorRepository();

    // Aportan las consultas con JOIN y la lista completa de proveedores.
    private final ProveedorRepository proveedorRepository =
            new ProveedorRepository();
    private final ProductoRepository productoRepository =
            new ProductoRepository();

    /**
     * @param codigoProducto código del producto
     * @return los proveedores que surten ese producto (lista vacía si no tiene)
     */
    public List<Proveedor> listarProveedoresDeProducto(String codigoProducto) {
        Validador.validarTexto(codigoProducto, "Producto");
        return proveedorRepository.obtenerPorProducto(codigoProducto);
    }

    /**
     * Proveedores que todavía NO están asociados al producto. Sirve para
     * llenar el selector de la pantalla sin ofrecer pares que ya existen.
     *
     * @param codigoProducto código del producto
     * @return todos los proveedores menos los que ya lo surten
     */
    public List<Proveedor> listarProveedoresDisponibles(String codigoProducto) {
        Validador.validarTexto(codigoProducto, "Producto");

        Set<String> yaAsociados = new HashSet<>();
        for (Proveedor p : proveedorRepository.obtenerPorProducto(codigoProducto)) {
            yaAsociados.add(p.getId());
        }

        List<Proveedor> disponibles = new ArrayList<>();
        for (Proveedor p : proveedorRepository.obtenerTodos()) {
            if (!yaAsociados.contains(p.getId())) {
                disponibles.add(p);
            }
        }
        return disponibles;
    }

    /**
     * Asocia un proveedor con un producto.
     *
     * @throws IllegalArgumentException si falta algún dato o ya estaban asociados
     */
    public void asociar(String codigoProducto, String idProveedor) {
        Validador.validarTexto(codigoProducto, "Producto");
        Validador.validarTexto(idProveedor, "Proveedor");

        if (asociacionRepository.existeAsociacion(codigoProducto, idProveedor)) {
            throw new IllegalArgumentException(
                    "Ese proveedor ya está asociado a este producto");
        }
        asociacionRepository.asociar(codigoProducto, idProveedor);
    }

    /**
     * Quita la asociación entre un proveedor y un producto.
     *
     * @throws IllegalArgumentException si falta algún dato o no estaban asociados
     */
    public void desasociar(String codigoProducto, String idProveedor) {
        Validador.validarTexto(codigoProducto, "Producto");
        Validador.validarTexto(idProveedor, "Proveedor");

        // desasociar() devuelve false si no había ninguna fila que borrar.
        if (!asociacionRepository.desasociar(codigoProducto, idProveedor)) {
            throw new IllegalArgumentException(
                    "Ese proveedor no está asociado a este producto");
        }
    }
}