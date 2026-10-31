package com.tiendatecnologica.service;

import com.tiendatecnologica.model.Proveedor;
import com.tiendatecnologica.repository.ProveedorRepository;

import java.util.List;

/**
 * Servicio encargado de gestionar las operaciones de los proveedores.
 *
 * Contiene las validaciones de negocio antes de utilizar el repositorio
 * para guardar, modificar, consultar o eliminar proveedores.
 */
public class ProveedorService {

    // CAMBIO SQL: ahora el service guarda y lee los proveedores en MySQL.
    // El nombre de la variable se mantiene para no tocar el resto de la clase.
    private final ProveedorRepository proveedorRepository =
            new ProveedorRepository();

    // CAMBIO ID: prefijo con el que se generan los ids (PROV-00001, PROV-00002...).
    private static final String PREFIJO_ID = "PROV-";

    /**
     * Obtiene todos los proveedores registrados.
     *
     * @return lista de proveedores
     */
    public List<Proveedor> obtenerProveedores() {
        return proveedorRepository.obtenerTodos();
    }

    /**
     * Registra un nuevo proveedor.
     *
     * El id NO lo digita el usuario: el service lo genera con el siguiente
     * número libre de la base de datos y se lo asigna al proveedor recibido,
     * así quien llamó puede leerlo después con {@code proveedor.getId()}.
     *
     * @param proveedor proveedor que se desea registrar (sin id)
     */
    public void registrarProveedor(Proveedor proveedor) {
        // CAMBIO ID: antes se validaba el id escrito por el usuario y se revisaba
        // si ya existía. Ahora solo se valida el nombre, porque el id lo genera
        // el sistema. Se valida primero para no consultar la base si los datos
        // ya están mal.
        validarNombre(proveedor);

        // CAMBIO ID: siguienteId() consulta el mayor número usado en la tabla
        // proveedor y devuelve el siguiente con formato PROV-00001.
        proveedor.setId(proveedorRepository.siguienteId(PREFIJO_ID));

        // Ya no hace falta existe(): el id recién generado no puede estar repetido.
        // Si por alguna razón lo estuviera, la clave primaria de MySQL lo rechaza
        // y RepositorioException muestra el mensaje.
        proveedorRepository.guardar(proveedor);
    }

    /**
     * Modifica los datos de un proveedor existente.
     *
     * El ID no se modifica porque funciona como clave del proveedor.
     *
     * @param proveedor proveedor con los nuevos datos
     */
    public void actualizarProveedor(Proveedor proveedor) {
        validarDatos(proveedor);

        if (!proveedorRepository.existe(proveedor.getId())) {
            throw new IllegalArgumentException(
                    "No existe un proveedor con el ID " + proveedor.getId()
            );
        }

        proveedorRepository.actualizar(proveedor);
    }

    /**
     * Elimina un proveedor existente.
     *
     * @param id identificador del proveedor
     */
    public void eliminarProveedor(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un proveedor"
            );
        }

        if (!proveedorRepository.existe(id)) {
            throw new IllegalArgumentException(
                    "No existe un proveedor con el ID " + id
            );
        }

        proveedorRepository.eliminar(id);
    }

    /**
     * Valida los datos básicos de un proveedor ya existente
     * (nombre e id obligatorios). Se usa al actualizar.
     *
     * @param proveedor proveedor que se desea validar
     */
    private void validarDatos(Proveedor proveedor) {
        validarNombre(proveedor);

        if (proveedor.getId() == null || proveedor.getId().isBlank()) {
            throw new IllegalArgumentException(
                    "El ID del proveedor es obligatorio"
            );
        }
    }

    /**
     * CAMBIO ID: validación separada solo del proveedor y su nombre.
     * Se usa al registrar (donde todavía no hay id) y dentro de validarDatos.
     *
     * @param proveedor proveedor que se desea validar
     */
    private void validarNombre(Proveedor proveedor) {
        if (proveedor == null) {
            throw new IllegalArgumentException(
                    "El proveedor no puede ser nulo"
            );
        }

        if (proveedor.getNombre() == null || proveedor.getNombre().isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor es obligatorio"
            );
        }
    }
}