package com.tiendatecnologica.repository;

import java.util.List;

/**
 * Contrato para repositorios de registros inmutables (bitácoras,
 * historiales). A diferencia de {@link Repository}, no permite actualizar
 * ni eliminar: una vez creado un registro queda fijo en el historial.
 * Se separa de Repository porque, aunque ambos leen y escriben en la base de datos, sus
 * entidades no comparten familia (ej. MovimientoInventario no es un
 * Producto) ni el mismo comportamiento (un movimiento no se edita).
 *
 * @param <T> tipo del registro que administra el repositorio
 */
public interface RegistroRepository<T> {

    /**
     * Obtiene todos los registros almacenados.
     *
     * @return lista con todos los registros; vacía si no hay ninguno
     */
    List<T> obtenerTodos();

    /**
     * Agrega un nuevo registro al historial.
     *
     * @param registro registro a agregar
     */
    void guardar(T registro);
}
