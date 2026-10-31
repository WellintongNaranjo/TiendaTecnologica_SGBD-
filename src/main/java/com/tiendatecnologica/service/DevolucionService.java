package com.tiendatecnologica.service;

import com.tiendatecnologica.enums.EstadoDevolucion;
import com.tiendatecnologica.model.DetalleVenta;
import com.tiendatecnologica.model.Devolucion;
import com.tiendatecnologica.model.Venta;
import com.tiendatecnologica.persistence.Transaccion;
import com.tiendatecnologica.repository.DevolucionRepository;
import com.tiendatecnologica.util.GeneradorId;
import com.tiendatecnologica.util.Validador;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio de devoluciones: verifica que el producto y la cantidad
 * reclamados realmente correspondan a lo comprado en la venta (descontando
 * lo ya devuelto anteriormente para esa misma venta), controla el flujo de
 * estados y, cuando una devolución se aprueba, reintegra el inventario a
 * través de {@link InventarioService} (nunca tocando el producto
 * directamente, siguiendo la misma regla de oro que usa {@code VentaService}).
 */
public class DevolucionService {

    private final DevolucionRepository devolucionRepository = new DevolucionRepository();
    private final VentaService ventaService = new VentaService();
    private final InventarioService inventarioService = new InventarioService();

    /**
     * Registra una nueva solicitud de devolución de una cantidad de producto
     * comprado en una venta, validando que la venta exista, que el producto
     * se haya comprado en ella y que la cantidad solicitada no supere lo
     * comprado menos lo que ya esté en trámite o aprobado de solicitudes
     * anteriores para esa misma venta y producto.
     *
     * @param idVenta id de la venta en la que se compró el producto
     * @param codigoProducto código del producto que se quiere devolver
     * @param cantidad cantidad de unidades a devolver (debe ser mayor que 0)
     * @param motivo razón por la que el cliente solicita la devolución
     * @return la solicitud de devolución ya registrada, con estado PENDIENTE
     * @throws IllegalArgumentException si algún dato es inválido, la venta no existe o está anulada,
     *         el producto no fue comprado en ella o la cantidad excede lo disponible para devolver
     */
    public Devolucion registrarDevolucion(String idVenta, String codigoProducto, int cantidad, String motivo) {
        Validador.validarTexto(idVenta, "Venta");
        Validador.validarTexto(codigoProducto, "Código de producto");
        Validador.validarTexto(motivo, "Motivo");
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a devolver debe ser mayor que 0");
        }

        Venta venta = ventaService.buscarVenta(idVenta);
        if (venta == null) {
            throw new IllegalArgumentException("No existe la venta " + idVenta);
        }
        if (!venta.estaCompletada()) {
            throw new IllegalArgumentException("La venta " + idVenta + " está anulada");
        }

        int cantidadComprada = cantidadCompradaEn(idVenta, codigoProducto);
        if (cantidadComprada == 0) {
            throw new IllegalArgumentException(
                    "El producto " + codigoProducto + " no fue comprado en la venta " + idVenta);
        }

        int cantidadYaSolicitada = cantidadYaSolicitadaPara(idVenta, codigoProducto);
        if (cantidad > cantidadComprada - cantidadYaSolicitada) {
            throw new IllegalArgumentException(
                    "Solo puede devolver " + (cantidadComprada - cantidadYaSolicitada)
                            + " unidad(es) más de " + codigoProducto + " para esta venta");
        }

        Devolucion devolucion = new Devolucion(
                GeneradorId.generarIdDevolucion(), idVenta, codigoProducto, cantidad,
                LocalDateTime.now(), motivo, EstadoDevolucion.PENDIENTE);
        devolucionRepository.guardar(devolucion);
        return devolucion;
    }

    /**
     * Avanza el estado de una solicitud de devolución, validando que la
     * transición sea válida según el flujo definido: PENDIENTE {@literal ->}
     * EN_REVISION {@literal ->} (APROBADA o RECHAZADA) {@literal ->} FINALIZADA.
     * Al pasar a APROBADA, la cantidad devuelta se reintegra automáticamente
     * al inventario del producto. El cambio de estado y el reingreso al
     * inventario se guardan en una sola transacción: o se hacen los dos o
     * no se hace ninguno.
     *
     * @param idDevolucion id de la solicitud a actualizar
     * @param nuevoEstado estado al que se quiere avanzar
     * @throws IllegalArgumentException si la solicitud no existe
     * @throws IllegalStateException si la transición de estado no es válida
     */
    public void avanzarEstado(String idDevolucion, EstadoDevolucion nuevoEstado) {
        Devolucion devolucion = devolucionRepository.buscarPorClave(idDevolucion);
        if (devolucion == null) {
            throw new IllegalArgumentException("No existe la devolución " + idDevolucion);
        }
        validarTransicion(devolucion.getEstado(), nuevoEstado);

        Transaccion.ejecutar(con -> {
            devolucionRepository.actualizarEstado(con, devolucion.getId(), nuevoEstado);
            if (nuevoEstado == EstadoDevolucion.APROBADA) {
                inventarioService.registrarEntrada(
                        con, devolucion.getCodigoProducto(), devolucion.getCantidad(),
                        "Devolución " + devolucion.getId());
            }
            return null;
        });
    }

    /**
     * Elimina una solicitud de devolución. Solo se permite si todavía está en
     * estado PENDIENTE: una devolución aprobada ya reintegró inventario y las
     * demás forman parte del historial, por lo que no deben borrarse.
     *
     * @param idDevolucion id de la solicitud a eliminar
     * @throws IllegalArgumentException si la solicitud no existe
     * @throws IllegalStateException si la solicitud ya no está PENDIENTE
     */
    public void eliminarDevolucion(String idDevolucion) {
        Devolucion devolucion = devolucionRepository.buscarPorClave(idDevolucion);
        if (devolucion == null) {
            throw new IllegalArgumentException("No existe la devolución " + idDevolucion);
        }
        if (devolucion.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede eliminar una devolución PENDIENTE (esta está " + devolucion.getEstado() + ")");
        }
        devolucionRepository.eliminar(idDevolucion);
    }

    /**
     * Lista las solicitudes de devolución en un estado dado. El filtro lo aplica MySQL.
     *
     * @param estado estado a filtrar; null equivale a "Todos" (sin filtro)
     * @return las solicitudes de devolución que están en ese estado
     */
    public List<Devolucion> listarDevolucionesPorEstado(EstadoDevolucion estado) {
        return devolucionRepository.obtenerPorEstado(estado);
    }

    /**
     * Suma la cantidad comprada de un producto específico dentro de una venta.
     *
     * @param idVenta id de la venta a revisar
     * @param codigoProducto código de producto a sumar entre las líneas de esa venta
     * @return cantidad total comprada de ese producto en esa venta (0 si no aparece)
     */
    private int cantidadCompradaEn(String idVenta, String codigoProducto) {
        int total = 0;
        for (DetalleVenta detalle : ventaService.obtenerDetalle(idVenta)) {
            if (detalle.getCodigoProducto().equals(codigoProducto)) {
                total += detalle.getCantidad();
            }
        }
        return total;
    }

    /**
     * Suma la cantidad ya solicitada en devoluciones previas (en cualquier
     * estado que no sea RECHAZADA) de un producto para una venta, para no
     * permitir devolver más unidades de las que realmente se compraron.
     *
     * @param idVenta id de la venta a revisar
     * @param codigoProducto código de producto a sumar entre devoluciones previas
     * @return cantidad ya solicitada previamente
     */
    private int cantidadYaSolicitadaPara(String idVenta, String codigoProducto) {
        int total = 0;
        for (Devolucion d : devolucionRepository.obtenerPorVenta(idVenta)) {
            if (d.getCodigoProducto().equals(codigoProducto) && d.getEstado() != EstadoDevolucion.RECHAZADA) {
                total += d.getCantidad();
            }
        }
        return total;
    }

    /**
     * Valida que se pueda pasar de un estado a otro según el flujo definido
     * para las devoluciones.
     *
     * @param actual estado actual de la solicitud
     * @param nuevo estado al que se quiere avanzar
     * @throws IllegalStateException si la transición no está permitida
     */
    private void validarTransicion(EstadoDevolucion actual, EstadoDevolucion nuevo) {
        boolean valida = switch (actual) {
            case PENDIENTE -> nuevo == EstadoDevolucion.EN_REVISION;
            case EN_REVISION -> nuevo == EstadoDevolucion.APROBADA || nuevo == EstadoDevolucion.RECHAZADA;
            case APROBADA, RECHAZADA -> nuevo == EstadoDevolucion.FINALIZADA;
            case FINALIZADA -> false;
        };
        if (!valida) {
            throw new IllegalStateException(
                    "No se puede pasar la devolución de " + actual + " a " + nuevo);
        }
    }
}
