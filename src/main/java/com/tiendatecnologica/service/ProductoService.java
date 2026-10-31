package com.tiendatecnologica.service;

import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.util.Validador;

import java.util.ArrayList;
import java.util.List;

import com.tiendatecnologica.repository.ProductoRepository;

/**
 * Lógica de negocio y validaciones sobre productos. Los Controllers hablan
 * con esta clase, nunca directamente con ProductoJDBC_Repository.
 */
public class ProductoService {
    /** Repositorio que guarda y lee los productos en MySQL. */
    private final ProductoRepository productoRepository = new ProductoRepository();

    /**
     * Registra un nuevo producto, validando sus datos y que el código no exista.
     *
     * @param producto producto a registrar
     * @throws IllegalArgumentException si algún dato es inválido o el código ya existe
     */
    public void registrarProducto(Producto producto) {
        validar(producto);
        if (productoRepository.existe(producto.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código " + producto.getCodigo());
        }
        productoRepository.guardar(producto);
    }

    /**
     * Modifica un producto existente.
     *
     * @param producto producto con los datos actualizados
     * @throws IllegalArgumentException si algún dato es inválido o el código no existe
     */
    public void modificarProducto(Producto producto) {
        validar(producto);
        if (!productoRepository.existe(producto.getCodigo())) {
            throw new IllegalArgumentException("No existe un producto con el código " + producto.getCodigo());
        }
        productoRepository.actualizar(producto);
    }

    /**
     * Elimina un producto por su código.
     *
     * @param codigo código del producto a eliminar
     * @throws IllegalArgumentException si el código no existe
     */
    public void eliminarProducto(String codigo) {
        if (!productoRepository.existe(codigo)) {
            throw new IllegalArgumentException("No existe un producto con el código " + codigo);
        }
        productoRepository.eliminar(codigo);
    }

    /**
     * Busca un producto por su código.
     *
     * @param codigo código a buscar
     * @return el producto encontrado, o null si no existe
     */
    public Producto buscarProducto(String codigo) {
        return productoRepository.buscarPorClave(codigo);
    }

    /**
     * @return todos los productos registrados
     */
    public List<Producto> listarProductos() {
        return productoRepository.obtenerTodos();
    }

    /**
     * Busca productos cuyo código, nombre, categoría o marca contenga el texto dado.
     *
     * @param texto texto de búsqueda (no distingue mayúsculas/minúsculas)
     * @return lista de productos que coinciden
     */
    public List<Producto> buscarProductos(String texto) {
        String textoLower = texto == null ? "" : texto.toLowerCase();
        List<Producto> resultado = new ArrayList<>();

        for (Producto p : productoRepository.obtenerTodos()) {
            if (p.getNombre().toLowerCase().contains(textoLower)
                    || p.getCodigo().toLowerCase().contains(textoLower)
                    || p.getCategoria().toLowerCase().contains(textoLower)
                    || p.getMarca().toLowerCase().contains(textoLower)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * @return productos cuyo stock está en o por debajo del mínimo, pero no agotados
     */
    public List<Producto> obtenerProductosBajoStock() {
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : productoRepository.obtenerTodos()) {
            if (p.necesitaReabastecimiento() && p.getCantidadDisponible() > 0) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * @return productos con cantidad disponible igual a 0
     */
    public List<Producto> obtenerProductosAgotados() {
        List<Producto> resultado = new ArrayList<>();
        for (Producto p : productoRepository.obtenerTodos()) {
            if (!p.tieneStock()) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    /**
     * Valida los datos obligatorios de un producto.
     *
     * @param p producto a validar
     * @throws IllegalArgumentException si algún dato es inválido
     */
    private void validar(Producto p) {
        Validador.validarTexto(p.getCodigo(), "Código");
        Validador.validarTexto(p.getNombre(), "Nombre");
        Validador.validarTexto(p.getCategoria(), "Categoría");
        Validador.validarTexto(p.getMarca(), "Marca");
        Validador.validarPositivo(p.getPrecioCompra(), "Precio de compra");
        Validador.validarPositivo(p.getPrecioVenta(), "Precio de venta");
        Validador.validarNoNegativo(p.getCantidadDisponible(), "Cantidad disponible");
        Validador.validarNoNegativo(p.getStockMinimo(), "Stock mínimo");
        Validador.validarNoNegativo(p.getMesesGarantia(), "Meses de garantía");
    }
}
