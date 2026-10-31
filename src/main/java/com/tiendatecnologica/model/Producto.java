package com.tiendatecnologica.model;

/**
 * Representa un producto del catálogo de la tienda. Solo contiene datos y
 * comportamiento básico propio del producto; no sabe nada de MySQL ni de
 * cómo se guarda (eso es responsabilidad de ProductoJDBC_Repository).
 */
public class Producto {

    private String codigo;
    private String nombre;
    private String categoria;
    private String marca;
    private double precioCompra;
    private double precioVenta;
    private int cantidadDisponible;
    private int stockMinimo;
    private int mesesGarantia;

    /**
     * Crea un producto.
     *
     * @param codigo código único del producto
     * @param nombre nombre del producto
     * @param categoria categoría a la que pertenece
     * @param marca marca del producto
     * @param precioCompra precio al que se compra al proveedor
     * @param precioVenta precio al que se vende al cliente
     * @param cantidadDisponible cantidad actual en inventario
     * @param stockMinimo cantidad mínima antes de necesitar reabastecimiento
     * @param mesesGarantia meses de garantía que ofrece el producto
     */
    public Producto(String codigo, String nombre, String categoria, String marca,
                    double precioCompra, double precioVenta, int cantidadDisponible,
                    int stockMinimo, int mesesGarantia) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.marca = marca;
        this.precioCompra = precioCompra;
        this.precioVenta = precioVenta;
        this.cantidadDisponible = cantidadDisponible;
        this.stockMinimo = stockMinimo;
        this.mesesGarantia = mesesGarantia;
    }

    /** @return el código único del producto */
    public String getCodigo() {
        return codigo;
    }

    /** @param codigo nuevo código del producto */
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    /** @return el nombre del producto */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nuevo nombre del producto */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return la categoría del producto */
    public String getCategoria() {
        return categoria;
    }

    /** @param categoria nueva categoría del producto */
    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    /** @return la marca del producto */
    public String getMarca() {
        return marca;
    }

    /** @param marca nueva marca del producto */
    public void setMarca(String marca) {
        this.marca = marca;
    }

    /** @return el precio al que se compra al proveedor */
    public double getPrecioCompra() {
        return precioCompra;
    }

    /** @param precioCompra nuevo precio de compra */
    public void setPrecioCompra(double precioCompra) {
        this.precioCompra = precioCompra;
    }

    /** @return el precio al que se vende al cliente */
    public double getPrecioVenta() {
        return precioVenta;
    }

    /** @param precioVenta nuevo precio de venta */
    public void setPrecioVenta(double precioVenta) {
        this.precioVenta = precioVenta;
    }

    /** @return la cantidad actual disponible en inventario */
    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    /** @param cantidadDisponible nueva cantidad disponible */
    public void setCantidadDisponible(int cantidadDisponible) {
        this.cantidadDisponible = cantidadDisponible;
    }

    /** @return la cantidad mínima antes de necesitar reabastecimiento */
    public int getStockMinimo() {
        return stockMinimo;
    }

    /** @param stockMinimo nueva cantidad mínima */
    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    /** @return los meses de garantía que ofrece el producto */
    public int getMesesGarantia() {
        return mesesGarantia;
    }

    /** @param mesesGarantia nuevos meses de garantía */
    public void setMesesGarantia(int mesesGarantia) {
        this.mesesGarantia = mesesGarantia;
    }

    /**
     * Indica si el producto tiene stock disponible.
     *
     * @return true si la cantidad disponible es mayor que 0
     */
    public boolean tieneStock() {
        return cantidadDisponible > 0;
    }

    /**
     * Indica si el producto necesita reabastecimiento.
     *
     * @return true si la cantidad disponible es menor o igual al stock mínimo
     */
    public boolean necesitaReabastecimiento() {
        return cantidadDisponible <= stockMinimo;
    }

    /** @return una representación legible del producto, útil para depurar */
    @Override
    public String toString() {
        return codigo + " - " + nombre + " (" + marca + ") | stock: " + cantidadDisponible;
    }
}
