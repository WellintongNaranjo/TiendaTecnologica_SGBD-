package com.tiendatecnologica.model;

import com.tiendatecnologica.enums.EstadoGarantia;

import java.time.LocalDateTime;

/**
 * Representa una solicitud de garantía sobre un producto vendido.
 * Solo contiene datos propios de la solicitud; no sabe nada de MySQL
 * (eso es responsabilidad de {@code GarantiaRepository}) ni de cómo
 * se valida contra la venta original (eso lo hace {@code GarantiaService}).
 */
public class Garantia {

    private String id;
    private String idVenta;
    private String codigoProducto;
    private LocalDateTime fechaSolicitud;
    private String descripcionProblema;
    private EstadoGarantia estado;

    /**
     * Crea una solicitud de garantía.
     *
     * @param id identificador único generado por el sistema (ej. GAR-00001)
     * @param idVenta id de la venta en la que se compró el producto
     * @param codigoProducto código del producto sobre el que se reclama garantía
     * @param fechaSolicitud fecha y hora en que se generó la solicitud
     * @param descripcionProblema descripción del problema reportado por el cliente
     * @param estado estado actual de la solicitud
     */
    public Garantia(String id, String idVenta, String codigoProducto, LocalDateTime fechaSolicitud,
                    String descripcionProblema, EstadoGarantia estado) {
        this.id = id;
        this.idVenta = idVenta;
        this.codigoProducto = codigoProducto;
        this.fechaSolicitud = fechaSolicitud;
        this.descripcionProblema = descripcionProblema;
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

    /** @return el código del producto reclamado */
    public String getCodigoProducto() {
        return codigoProducto;
    }

    /** @param codigoProducto nuevo código de producto */
    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    /** @return la fecha y hora en que se generó la solicitud */
    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    /** @param fechaSolicitud nueva fecha de solicitud */
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    /** @return la descripción del problema reportado */
    public String getDescripcionProblema() {
        return descripcionProblema;
    }

    /** @param descripcionProblema nueva descripción del problema */
    public void setDescripcionProblema(String descripcionProblema) {
        this.descripcionProblema = descripcionProblema;
    }

    /** @return el estado actual de la solicitud */
    public EstadoGarantia getEstado() {
        return estado;
    }

    /** @param estado nuevo estado de la solicitud */
    public void setEstado(EstadoGarantia estado) {
        this.estado = estado;
    }

    /** @return una representación legible de la solicitud, útil para depurar */
    @Override
    public String toString() {
        return id + " - producto " + codigoProducto + " (venta " + idVenta + ") | " + estado;
    }
}
