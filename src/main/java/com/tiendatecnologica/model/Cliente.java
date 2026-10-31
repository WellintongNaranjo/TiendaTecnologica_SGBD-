package com.tiendatecnologica.model;

/**
 * Representa a un cliente que puede realizar compras en la tienda.
 * Solo contiene datos propios del cliente; no sabe nada de MySQL
 * (eso es responsabilidad de {@code ClienteJDBC_Repository}).
 */
public class Cliente {

    private String id;
    private String cedula;
    private String nombre;
    private String telefono;
    private String email;

    /**
     * Crea un cliente.
     *
     * @param id identificador único generado por el sistema (ej. CLI-00001)
     * @param cedula número de identificación (cédula) del cliente
     * @param nombre nombre completo del cliente
     * @param telefono teléfono de contacto (8 dígitos)
     * @param email correo electrónico del cliente
     */
    public Cliente(String id, String cedula, String nombre, String telefono, String email) {
        this.id = id;
        this.cedula = cedula;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
    }

    /** @return el identificador único del cliente */
    public String getId() {
        return id;
    }

    /** @param id nuevo identificador del cliente */
    public void setId(String id) {
        this.id = id;
    }

    /** @return la cédula del cliente */
    public String getCedula() {
        return cedula;
    }

    /** @param cedula nueva cédula del cliente */
    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    /** @return el nombre completo del cliente */
    public String getNombre() {
        return nombre;
    }

    /** @param nombre nuevo nombre del cliente */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /** @return el teléfono del cliente */
    public String getTelefono() {
        return telefono;
    }

    /** @param telefono nuevo teléfono del cliente */
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    /** @return el correo electrónico del cliente */
    public String getEmail() {
        return email;
    }

    /** @param email nuevo correo electrónico del cliente */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return una representación legible del cliente, útil para depurar */
    @Override
    public String toString() {
        return id + " - " + nombre + " (" + cedula + ")";
    }
}
