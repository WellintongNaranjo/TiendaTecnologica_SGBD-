package com.tiendatecnologica.service;

import com.tiendatecnologica.enums.EstadoGarantia;
import com.tiendatecnologica.model.DetalleVenta;
import com.tiendatecnologica.model.Garantia;
import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.Venta;
import com.tiendatecnologica.repository.GarantiaRepository;
import com.tiendatecnologica.util.GeneradorId;
import com.tiendatecnologica.util.Validador;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio de garantías: verifica que el producto reclamado
 * realmente se haya comprado en la venta indicada y que la solicitud se
 * haga dentro del período de garantía del producto, y controla que el
 * estado de una solicitud solo avance hacia adelante (nunca retroceda ni
 * salte pasos), igual que las reglas de flujo de estados del resto del
 * proyecto (ej. {@code VentaService}, órdenes de compra).
 */
public class GarantiaService {

    private final GarantiaRepository garantiaRepository = new GarantiaRepository();
    private final VentaService ventaService = new VentaService();
    private final ProductoService productoService = new ProductoService();

    /**
     * Registra una nueva solicitud de garantía para un producto de una venta,
     * validando que la venta exista y no esté anulada, que el producto exista, que el producto se haya
     * comprado efectivamente en esa venta y que la solicitud se haga dentro
     * del período de garantía (fecha de venta + meses de garantía del producto).
     *
     * @param idVenta id de la venta en la que se compró el producto
     * @param codigoProducto código del producto sobre el que se reclama
     * @param descripcionProblema descripción del problema reportado por el cliente
     * @return la solicitud de garantía ya registrada, con estado PENDIENTE
     * @throws IllegalArgumentException si algún dato es inválido, la venta no existe o está
     *         anulada, o el producto no fue comprado en esa venta
     * @throws IllegalStateException si el período de garantía ya venció
     */
    public Garantia registrarGarantia(String idVenta, String codigoProducto, String descripcionProblema) {
        Validador.validarTexto(idVenta, "Venta");
        Validador.validarTexto(codigoProducto, "Código de producto");
        Validador.validarTexto(descripcionProblema, "Descripción del problema");

        Venta venta = ventaService.buscarVenta(idVenta);
        if (venta == null) {
            throw new IllegalArgumentException("No existe la venta " + idVenta);
        }
        if (!venta.estaCompletada()) {
            throw new IllegalArgumentException("La venta " + idVenta + " está anulada");
        }

        Producto producto = productoService.buscarProducto(codigoProducto);
        if (producto == null) {
            throw new IllegalArgumentException("No existe el producto " + codigoProducto);
        }

        if (!productoFueVendidoEn(idVenta, codigoProducto)) {
            throw new IllegalArgumentException(
                    "El producto " + codigoProducto + " no fue comprado en la venta " + idVenta);
        }

        LocalDateTime limiteGarantia = venta.getFecha().plusMonths(producto.getMesesGarantia());
        if (LocalDateTime.now().isAfter(limiteGarantia)) {
            throw new IllegalStateException(
                    "El período de garantía de " + producto.getNombre() + " venció el " + limiteGarantia);
        }

        Garantia garantia = new Garantia(
                GeneradorId.generarIdGarantia(), idVenta, codigoProducto,
                LocalDateTime.now(), descripcionProblema, EstadoGarantia.PENDIENTE);
        garantiaRepository.guardar(garantia);
        return garantia;
    }

    /**
     * Avanza el estado de una solicitud de garantía, validando que la
     * transición sea válida según el flujo definido: PENDIENTE {@literal ->}
     * EN_REVISION {@literal ->} (APROBADA o RECHAZADA) {@literal ->} FINALIZADA.
     * No se permite retroceder ni saltar estados.
     *
     * @param idGarantia id de la solicitud a actualizar
     * @param nuevoEstado estado al que se quiere avanzar
     * @throws IllegalArgumentException si la solicitud no existe
     * @throws IllegalStateException si la transición de estado no es válida
     */
    public void avanzarEstado(String idGarantia, EstadoGarantia nuevoEstado) {
        Garantia garantia = garantiaRepository.buscarPorClave(idGarantia);
        if (garantia == null) {
            throw new IllegalArgumentException("No existe la garantía " + idGarantia);
        }
        validarTransicion(garantia.getEstado(), nuevoEstado);
        garantia.setEstado(nuevoEstado);
        garantiaRepository.actualizar(garantia);
    }

    /**
     * Elimina una solicitud de garantía. Solo se permite si todavía está en
     * estado PENDIENTE: una vez que entra al flujo de revisión forma parte del
     * historial y no debe borrarse.
     *
     * @param idGarantia id de la solicitud a eliminar
     * @throws IllegalArgumentException si la solicitud no existe
     * @throws IllegalStateException si la solicitud ya no está PENDIENTE
     */
    public void eliminarGarantia(String idGarantia) {
        Garantia garantia = garantiaRepository.buscarPorClave(idGarantia);
        if (garantia == null) {
            throw new IllegalArgumentException("No existe la garantía " + idGarantia);
        }
        if (garantia.getEstado() != EstadoGarantia.PENDIENTE) {
            throw new IllegalStateException(
                    "Solo se puede eliminar una garantía PENDIENTE (esta está " + garantia.getEstado() + ")");
        }
        garantiaRepository.eliminar(idGarantia);
    }

    /**
     * Lista las solicitudes de garantía en un estado dado. El filtro lo aplica MySQL.
     *
     * @param estado estado a filtrar; null equivale a "Todos" (sin filtro)
     * @return las solicitudes de garantía que están en ese estado
     */
    public List<Garantia> listarGarantiasPorEstado(EstadoGarantia estado) {
        return garantiaRepository.obtenerPorEstado(estado);
    }

    /**
     * Verifica que un producto haya sido efectivamente comprado en una venta.
     *
     * @param idVenta id de la venta a revisar
     * @param codigoProducto código de producto a buscar entre las líneas de esa venta
     * @return true si el producto aparece en el detalle de esa venta
     */
    private boolean productoFueVendidoEn(String idVenta, String codigoProducto) {
        for (DetalleVenta detalle : ventaService.obtenerDetalle(idVenta)) {
            if (detalle.getCodigoProducto().equals(codigoProducto)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valida que se pueda pasar de un estado a otro según el flujo definido
     * para las garantías.
     *
     * @param actual estado actual de la solicitud
     * @param nuevo estado al que se quiere avanzar
     * @throws IllegalStateException si la transición no está permitida
     */
    private void validarTransicion(EstadoGarantia actual, EstadoGarantia nuevo) {
        boolean valida = switch (actual) {
            case PENDIENTE -> nuevo == EstadoGarantia.EN_REVISION;
            case EN_REVISION -> nuevo == EstadoGarantia.APROBADA || nuevo == EstadoGarantia.RECHAZADA;
            case APROBADA, RECHAZADA -> nuevo == EstadoGarantia.FINALIZADA;
            case FINALIZADA -> false;
        };
        if (!valida) {
            throw new IllegalStateException(
                    "No se puede pasar la garantía de " + actual + " a " + nuevo);
        }
    }
}
