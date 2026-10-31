package com.tiendatecnologica.model;

import com.tiendatecnologica.enums.MetodoPago;

/**
 * Representa el pago asociado a una venta.
 *
 * Almacena el método de pago utilizado, el monto recibido del cliente
 * y el vuelto que debe entregarse.
 *
 * Estos datos se almacenan como columnas dentro de la tabla {@code venta}
 * mediante {@code VentaRepository}.
 *
 * La clase concentra la validación y el cálculo relacionados con el pago,
 * evitando repetir esta lógica en los servicios.
 */
public class Pago {

    /** Método utilizado para realizar el pago. */
    private final MetodoPago metodoPago;

    /** Monto entregado por el cliente. */
    private final double montoRecibido;

    /** Vuelto que debe entregarse al cliente. */
    private final double vuelto;

    /**
     * Crea un nuevo pago y valida que el monto recibido sea suficiente
     * para cubrir el total de la venta.
     *
     * @param metodoPago método utilizado para pagar
     * @param montoRecibido monto que entrega el cliente
     * @param total total a cobrar de la venta
     * @throws IllegalArgumentException si no se indica un método de pago
     *         o si el monto recibido es menor que el total
     */
    public Pago(
            MetodoPago metodoPago,
            double montoRecibido,
            double total
    ) {
        if (metodoPago == null) {
            throw new IllegalArgumentException(
                    "Debe indicar un método de pago"
            );
        }

        if (montoRecibido < total) {
            throw new IllegalArgumentException(
                    "El monto recibido (" + montoRecibido
                            + ") es menor que el total a pagar ("
                            + total + ")"
            );
        }

        this.metodoPago = metodoPago;
        this.montoRecibido = montoRecibido;
        this.vuelto = redondear(montoRecibido - total);
    }

    /**
     * Constructor privado utilizado para reconstruir un pago
     * previamente calculado al leer una venta desde la base de datos.
     *
     * En este caso, el vuelto ya fue calculado y almacenado,
     * por lo que no es necesario repetir la validación de negocio.
     *
     * @param metodoPago método utilizado para pagar
     * @param montoRecibido monto recibido del cliente
     * @param vuelto vuelto que ya había sido calculado
     * @param reconstruccion parámetro utilizado para diferenciar
     *                        este constructor del constructor principal
     */
    private Pago(
            MetodoPago metodoPago,
            double montoRecibido,
            double vuelto,
            boolean reconstruccion
    ) {
        this.metodoPago = metodoPago;
        this.montoRecibido = montoRecibido;
        this.vuelto = vuelto;
    }

    /**
     * Reconstruye un pago que ya fue validado y almacenado.
     *
     * Se utiliza al leer una venta desde la base de datos,
     * conservando el vuelto que fue calculado originalmente.
     *
     * @param metodoPago método utilizado para pagar
     * @param montoRecibido monto recibido del cliente
     * @param vuelto vuelto almacenado en el archivo
     * @return objeto {@code Pago} reconstruido
     */
    public static Pago reconstruir(
            MetodoPago metodoPago,
            double montoRecibido,
            double vuelto
    ) {
        return new Pago(
                metodoPago,
                montoRecibido,
                vuelto,
                true
        );
    }

    /**
     * Obtiene el método utilizado para realizar el pago.
     *
     * @return método de pago
     */
    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    /**
     * Obtiene el monto entregado por el cliente.
     *
     * @return monto recibido
     */
    public double getMontoRecibido() {
        return montoRecibido;
    }

    /**
     * Obtiene el vuelto que debe entregarse al cliente.
     *
     * @return monto del vuelto
     */
    public double getVuelto() {
        return vuelto;
    }

    /**
     * Redondea un valor monetario a dos posiciones decimales.
     *
     * @param valor valor que se desea redondear
     * @return valor redondeado a dos decimales
     */
    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    /**
     * Devuelve una representación textual del pago.
     *
     * @return método de pago, monto recibido y vuelto
     */
    @Override
    public String toString() {
        return metodoPago
                + " | recibido: " + montoRecibido
                + " | vuelto: " + vuelto;
    }
}