package com.tiendatecnologica.model;
import com.tiendatecnologica.enums.EstadoVenta;

import java.time.LocalDateTime;

/**
 * Representa la cabecera de una venta (la "factura"): a quién se le
 * vendió, cuándo, los montos totales y cómo se pagó. Las líneas de
 * productos vendidos viven aparte, en {@link DetalleVenta} /
 * la tabla {@code detalle_venta}, igual que un producto no sabe de sus
 * movimientos de inventario.
 */
public class Venta {

    private String id;
    private LocalDateTime fecha;
    private String idCliente;
    private double subtotal;
    private double impuesto;
    private double total;
    private Pago pago;
    private EstadoVenta estado;

    /**
     * Crea una venta.
     *
     * @param id identificador único generado por el sistema (ej. V-00001)
     * @param fecha fecha y hora en que se realizó la venta
     * @param idCliente id del cliente al que se le vendió
     * @param subtotal suma de los subtotales de cada línea, sin impuesto
     * @param impuesto monto de impuesto aplicado sobre el subtotal
     * @param total subtotal + impuesto
     * @param pago información del pago (método, monto recibido, vuelto)
     * @param estado estado actual de la venta (completada o anulada)
     */
    public Venta(String id, LocalDateTime fecha, String idCliente, double subtotal,
                 double impuesto, double total, Pago pago, EstadoVenta estado) {
        this.id = id;
        this.fecha = fecha;
        this.idCliente = idCliente;
        this.subtotal = subtotal;
        this.impuesto = impuesto;
        this.total = total;
        this.pago = pago;
        this.estado = estado;
    }

    /** @return el identificador único de la venta */
    public String getId() {
        return id;
    }

    /** @param id nuevo identificador de la venta */
    public void setId(String id) {
        this.id = id;
    }

    /** @return la fecha y hora en que se realizó la venta */
    public LocalDateTime getFecha() {
        return fecha;
    }

    /** @param fecha nueva fecha de la venta */
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    /** @return el id del cliente al que se le vendió */
    public String getIdCliente() {
        return idCliente;
    }

    /** @param idCliente nuevo id de cliente */
    public void setIdCliente(String idCliente) {
        this.idCliente = idCliente;
    }

    /** @return el subtotal de la venta, sin impuesto */
    public double getSubtotal() {
        return subtotal;
    }

    /** @param subtotal nuevo subtotal */
    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    /** @return el monto de impuesto aplicado */
    public double getImpuesto() {
        return impuesto;
    }

    /** @param impuesto nuevo monto de impuesto */
    public void setImpuesto(double impuesto) {
        this.impuesto = impuesto;
    }

    /** @return el total de la venta (subtotal + impuesto) */
    public double getTotal() {
        return total;
    }

    /** @param total nuevo total */
    public void setTotal(double total) {
        this.total = total;
    }

    /** @return la información del pago de esta venta */
    public Pago getPago() {
        return pago;
    }

    /** @param pago nueva información de pago */
    public void setPago(Pago pago) {
        this.pago = pago;
    }

    /** @return el estado actual de la venta */
    public EstadoVenta getEstado() {
        return estado;
    }

    /** @param estado nuevo estado de la venta */
    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    /** @return true si la venta está completada (no anulada) */
    public boolean estaCompletada() {
        return estado == EstadoVenta.COMPLETADA;
    }

    /** @return una representación legible de la venta, útil para depurar */
    @Override
    public String toString() {
        return id + " - " + fecha + " | cliente: " + idCliente + " | total: " + total + " | " + estado;
    }
}
