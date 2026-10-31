package com.tiendatecnologica.model;

import com.tiendatecnologica.enums.TipoMovimiento;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un movimiento de inventario (entrada, salida o ajuste).
 * Es una clase abstracta: cada subtipo concreto ({@link EntradaInventario},
 * {@link SalidaInventario}, {@link AjusteInventario}) sabe calcular la nueva
 * cantidad de inventario a partir de la cantidad actual, evitando repetir
 * esa lógica.
 */
public abstract class MovimientoInventario {

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Identificador único del movimiento. */
    private final String id;

    /** Código del producto afectado. */
    private final String codigoProducto;

    /** Cantidad involucrada en el movimiento. */
    private int cantidad;

    /** Cantidad disponible antes del movimiento. */
    private int cantidadAnterior;

    /** Cantidad disponible después del movimiento. */
    private int cantidadNueva;

    /** Fecha y hora en que ocurrió el movimiento. */
    private final LocalDateTime fecha;

    /** Razón del movimiento. */
    private final String motivo;

    /**
     * Crea un movimiento de inventario.
     *
     * @param id identificador único del movimiento
     * @param codigoProducto código del producto afectado
     * @param cantidad cantidad involucrada en el movimiento
     * @param fecha fecha y hora en que ocurrió
     * @param motivo razón del movimiento
     */
    protected MovimientoInventario(String id, String codigoProducto,
                                   int cantidad,
                                   LocalDateTime fecha,
                                   String motivo) {
        this.id = id;
        this.codigoProducto = codigoProducto;
        this.cantidad = cantidad;
        this.fecha = fecha;
        this.motivo = motivo;
    }

    /**
     * Aplica este movimiento sobre la cantidad actual del producto.
     *
     * Guarda la cantidad anterior y calcula la nueva cantidad,
     * dejando ambos valores registrados en el movimiento.
     *
     * @param cantidadActual cantidad disponible antes del movimiento
     * @return nueva cantidad disponible después del movimiento
     */
    public int aplicar(int cantidadActual) {
        this.cantidadAnterior = cantidadActual;
        this.cantidadNueva =
                calcularNuevaCantidad(cantidadActual);

        return this.cantidadNueva;
    }

    /**
     * Calcula la nueva cantidad de inventario según el tipo
     * de movimiento.
     *
     * @param cantidadActual cantidad disponible antes del movimiento
     * @return cantidad disponible después del movimiento
     */
    protected abstract int calcularNuevaCantidad(int cantidadActual);

    /**
     * Obtiene el tipo de movimiento.
     *
     * @return tipo de movimiento
     */
    public abstract TipoMovimiento getTipo();

    /**
     * Fija los valores de cantidad anterior y nueva sin recalcularlos.
     *
     * Se utiliza al reconstruir un movimiento desde la base de datos,
     * donde estos valores ya fueron calculados y almacenados.
     *
     * @param cantidadAnterior cantidad antes del movimiento
     * @param cantidadNueva cantidad después del movimiento
     */
    public void fijarHistorial(int cantidadAnterior,
                               int cantidadNueva) {
        this.cantidadAnterior = cantidadAnterior;
        this.cantidadNueva = cantidadNueva;
    }

    /**
     * Fija la cantidad registrada del movimiento sin recalcularla.
     *
     * Se utiliza al reconstruir un movimiento desde la base de datos.
     *
     * @param cantidad cantidad registrada en el historial
     */
    public void fijarCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * @return identificador único del movimiento
     */
    public String getId() {
        return id;
    }

    /**
     * @return código del producto afectado
     */
    public String getCodigoProducto() {
        return codigoProducto;
    }

    /**
     * @return cantidad involucrada en el movimiento
     */
    public int getCantidad() {
        return cantidad;
    }

    /**
     * Permite a las subclases ajustar la cantidad involucrada.
     *
     * @param cantidad nueva cantidad a registrar
     */
    protected void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * @return cantidad disponible antes del movimiento
     */
    public int getCantidadAnterior() {
        return cantidadAnterior;
    }

    /**
     * @return cantidad disponible después del movimiento
     */
    public int getCantidadNueva() {
        return cantidadNueva;
    }

    /**
     * @return fecha y hora del movimiento
     */
    public LocalDateTime getFecha() {
        return fecha;
    }

    /**
     * @return razón del movimiento
     */
    public String getMotivo() {
        return motivo;
    }

    /**
     * @return representación legible del movimiento
     */
    @Override
    public String toString() {
        return getTipo() + " " + id
                + " - producto " + codigoProducto
                + ": " + cantidadAnterior
                + " -> " + cantidadNueva;
    }
}