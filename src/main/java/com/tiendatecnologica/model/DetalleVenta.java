package com.tiendatecnologica.model;

/**
 * Representa una línea del carrito / de una venta ya facturada: un
 * producto, la cantidad vendida y el precio al que se vendió (el precio
 * se copia en el momento de la venta para que si luego cambia el precio
 * del producto, las facturas viejas no se alteren).
 */
public class DetalleVenta {

    private String idVenta;
    private String codigoProducto;
    private int cantidad;
    private double precioUnitario;

    /** Nombre del producto, solo para mostrar en pantalla; no se guarda en la base de datos. */
    private String nombreProducto;

    /**
     * Crea una línea de detalle de venta.
     *
     * @param idVenta id de la venta a la que pertenece esta línea
     * @param codigoProducto código del producto vendido
     * @param cantidad cantidad vendida (debe ser mayor que 0)
     * @param precioUnitario precio unitario al momento de la venta
     */
    public DetalleVenta(String idVenta, String codigoProducto, int cantidad, double precioUnitario) {
        this.idVenta = idVenta;
        this.codigoProducto = codigoProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    /** @return el id de la venta a la que pertenece esta línea */
    public String getIdVenta() {
        return idVenta;
    }

    /** @param idVenta nuevo id de venta (se asigna una vez que la venta ya tiene id) */
    public void setIdVenta(String idVenta) {
        this.idVenta = idVenta;
    }

    /** @return el código del producto vendido */
    public String getCodigoProducto() {
        return codigoProducto;
    }

    /** @param codigoProducto nuevo código de producto */
    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    /** @return la cantidad vendida */
    public int getCantidad() {
        return cantidad;
    }

    /** @param cantidad nueva cantidad vendida */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /** @return el precio unitario al momento de la venta */
    public double getPrecioUnitario() {
        return precioUnitario;
    }

    /** @param precioUnitario nuevo precio unitario */
    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /** @return el nombre del producto (solo para mostrar en la UI) */
    public String getNombreProducto() {
        return nombreProducto;
    }

    /** @param nombreProducto nombre del producto, solo para mostrar en la UI */
    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    /** @return el subtotal de esta línea (cantidad * precioUnitario) */
    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    /** @return una representación legible de la línea, útil para depurar */
    @Override
    public String toString() {
        return codigoProducto + " x" + cantidad + " @ " + precioUnitario + " = " + getSubtotal();
    }
}
