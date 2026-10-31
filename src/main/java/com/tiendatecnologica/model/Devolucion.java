package com.tiendatecnologica.model;

import com.tiendatecnologica.enums.EstadoDevolucion;

import java.time.LocalDateTime;

/**
 * Representa una solicitud de devolución de una cantidad de un producto
 * comprado en una venta. Solo contiene datos propios de la solicitud;
 * no sabe nada de MySQL (eso es responsabilidad de {@code DevolucionRepository})
 * ni de cómo se valida o cómo afecta el inventario (eso lo hace
 * {@code DevolucionService}).
 */
public class Devolucion {

    private String id;
    private String idVenta;
    private String codigoProducto;
    private int cantidad;
    private LocalDateTime fechaSolicitud;
    private String motivo;
    private EstadoDevolucion estado;

    /**
     * Crea una solicitud de devolución.
     *
     * @param id identificador único generado por el sistema (ej. DEV-00001)
     * @param idVenta id de la venta en la que se compró el producto
     * @param codigoProducto código del producto que se quiere devolver
     * @param cantidad cantidad de unidades a devolver (debe ser mayor que 0)
     * @param fechaSolicitud fecha y hora en que se generó la solicitud
     * @param motivo razón por la que el cliente solicita la devolución
     * @param estado estado actual de la solicitud
     */
    public Devolucion(String id, String idVenta, String codigoProducto, int cantidad,
                      LocalDateTime fechaSolicitud, String motivo, EstadoDevolucion estado) {
        this.id = id;
        this.idVenta = idVenta;
        this.codigoProducto = codigoProducto;
        this.cantidad = cantidad;
        this.fechaSolicitud = fechaSolicitud;
        this.motivo = motivo;
        this.estado = estado;
    }

    /** @return el identificador único de la solicitud */
    public String getId() {
        return id;
    }

    /** @param id nuevo identificador de la solicitud */
    public void setId(String id) {
        this.id = id;
    }

    /** @return el id de la venta asociada */
    public String getIdVenta() {
        return idVenta;
    }

    /** @param idVenta nuevo id de venta asociado */
    public void setIdVenta(String idVenta) {
        this.idVenta = idVenta;
    }

    /** @return el código del producto a devolver */
    public String getCodigoProducto() {
        return codigoProducto;
    }

    /** @param codigoProducto nuevo código de producto */
    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    /** @return la cantidad de unidades a devolver */
    public int getCantidad() {
        return cantidad;
    }

    /** @param cantidad nueva cantidad a devolver */
    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /** @return la fecha y hora en que se generó la solicitud */
    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    /** @param fechaSolicitud nueva fecha de solicitud */
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    /** @return el motivo de la devolución */
    public String getMotivo() {
        return motivo;
    }

    /** @param motivo nuevo motivo de la devolución */
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    /** @return el estado actual de la solicitud */
    public EstadoDevolucion getEstado() {
        return estado;
    }

    /** @param estado nuevo estado de la solicitud */
    public void setEstado(EstadoDevolucion estado) {
        this.estado = estado;
    }

    /** @return una representación legible de la solicitud, útil para depurar */
    @Override
    public String toString() {
        return id + " - " + cantidad + "x " + codigoProducto + " (venta " + idVenta + ") | " + estado;
    }
}
