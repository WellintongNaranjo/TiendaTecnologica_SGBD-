package com.tiendatecnologica.service;

import com.tiendatecnologica.model.DetalleCompra;
import com.tiendatecnologica.model.OrdenCompra;
import com.tiendatecnologica.persistence.Transaccion;
import com.tiendatecnologica.repository.OrdenCompraRepository;

import java.util.List;

/**
 * Servicio encargado de gestionar las órdenes de compra.
 *
 * Se encarga de aplicar las reglas de negocio de una compra y de
 * coordinar el registro de la orden con la entrada de los productos
 * al inventario cuando la compra es recibida.
 */
public class CompraService {

    private final OrdenCompraRepository repositorio =
            new OrdenCompraRepository();

    private final InventarioService inventarioService =
            new InventarioService();

    /**
     * Registra una nueva orden de compra.
     *
     * Toda orden nueva comienza en estado Pendiente.
     * En este momento todavía no se modifica el inventario.
     *
     * @param orden orden de compra que se desea registrar
     */
    public void registrarCompra(OrdenCompra orden) {
        validarOrden(orden);

        // Toda orden nueva comienza como Pendiente.
        orden.setEstado("Pendiente");

        repositorio.guardar(orden);
    }

    /**
     * Obtiene todas las órdenes de compra registradas.
     *
     * @return lista de órdenes de compra
     */
    public List<OrdenCompra> obtenerCompras() {
        return repositorio.obtenerTodos();
    }

    /**
     * Marca una orden como recibida.
     *
     * Una compra solamente puede pasar de Pendiente a Recibida.
     * Al recibirla, se registra la entrada de cada producto
     * al inventario.
     *
     * @param idOrden identificador de la orden
     */
    public void recibirCompra(String idOrden) {

        OrdenCompra orden = obtenerCompraObligatoria(idOrden);

        validarTransicion(orden, "Recibida");

        /*
         * Todo ocurre en una sola transacción: si falla una entrada de
         * inventario o el cambio de estado, se revierte todo y la orden
         * sigue Pendiente (sin stock sumado a medias ni duplicado al reintentar).
         * registrarEntrada ya valida que el producto exista.
         */
        Transaccion.ejecutar(con -> {
            for (DetalleCompra detalle : orden.getDetalles()) {
                inventarioService.registrarEntrada(
                        con,
                        detalle.getIdProducto(),
                        detalle.getCantidad(),
                        "Compra " + orden.getIdOrden()
                );
            }
            repositorio.actualizarEstado(con, orden.getIdOrden(), "Recibida");
            return null;
        });

        orden.setEstado("Recibida");
    }

    /**
     * Cancela una orden de compra.
     *
     * Una compra solamente puede pasar de Pendiente a Cancelada.
     * Una compra cancelada no modifica el inventario.
     *
     * @param idOrden identificador de la orden
     */
    public void cancelarCompra(String idOrden) {

        OrdenCompra orden = obtenerCompraObligatoria(idOrden);

        validarTransicion(orden, "Cancelada");

        // Cancelar una compra no modifica el inventario.
        orden.setEstado("Cancelada");

        repositorio.actualizar(orden);
    }

    /**
     * Verifica que una orden exista.
     *
     * @param idOrden identificador de la orden
     * @return orden encontrada
     * @throws IllegalArgumentException si la orden no existe
     */
    private OrdenCompra obtenerCompraObligatoria(String idOrden) {

        OrdenCompra orden = repositorio.buscarPorClave(idOrden);

        if (orden == null) {
            throw new IllegalArgumentException(
                    "No existe la orden " + idOrden
            );
        }

        return orden;
    }

    /**
     * Verifica que el cambio de estado solicitado sea válido.
     *
     * Las únicas transiciones permitidas son:
     *
     * Pendiente -> Recibida
     * Pendiente -> Cancelada
     *
     * @param orden orden cuyo estado se desea cambiar
     * @param nuevoEstado nuevo estado solicitado
     */
    private void validarTransicion(
            OrdenCompra orden,
            String nuevoEstado) {

        if (!"Pendiente".equals(orden.getEstado())) {
            throw new IllegalArgumentException(
                    "Solo una compra Pendiente puede cambiar de estado."
            );
        }

        if (!"Recibida".equals(nuevoEstado)
                && !"Cancelada".equals(nuevoEstado)) {

            throw new IllegalArgumentException(
                    "El estado debe ser Recibida o Cancelada."
            );
        }
    }

    /**
     * Valida los datos necesarios para registrar una orden.
     *
     * @param orden orden que se desea validar
     */
    private void validarOrden(OrdenCompra orden) {

        if (orden == null) {
            throw new IllegalArgumentException(
                    "La orden de compra no puede ser nula"
            );
        }

        if (orden.getIdOrden() == null
                || orden.getIdOrden().isBlank()) {

            throw new IllegalArgumentException(
                    "El ID de la orden es obligatorio"
            );
        }

        if (orden.getIdProveedor() == null
                || orden.getIdProveedor().isBlank()) {

            throw new IllegalArgumentException(
                    "El ID del proveedor es obligatorio"
            );
        }

        if (orden.getFecha() == null
                || orden.getFecha().isBlank()) {

            throw new IllegalArgumentException(
                    "La fecha es obligatoria"
            );
        }

        if (orden.getDetalles() == null
                || orden.getDetalles().isEmpty()) {

            throw new IllegalArgumentException(
                    "La orden debe tener al menos un detalle"
            );
        }

        if (repositorio.existe(orden.getIdOrden())) {
            throw new IllegalArgumentException(
                    "Ya existe la orden " + orden.getIdOrden()
            );
        }

        for (DetalleCompra detalle : orden.getDetalles()) {

            if (detalle == null) {
                throw new IllegalArgumentException(
                        "La orden contiene un detalle inválido"
                );
            }

            if (detalle.getIdProducto() == null
                    || detalle.getIdProducto().isBlank()) {

                throw new IllegalArgumentException(
                        "El ID del producto es obligatorio"
                );
            }

            if (detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException(
                        "La cantidad debe ser mayor que 0"
                );
            }

            if (detalle.getCostoUnitario() < 0) {
                throw new IllegalArgumentException(
                        "El costo unitario no puede ser negativo"
                );
            }

            /*
             * Verifica que el producto exista antes de registrar
             * la orden.
             */
            inventarioService.obtenerExistencia(
                    detalle.getIdProducto()
            );
        }
    }
}