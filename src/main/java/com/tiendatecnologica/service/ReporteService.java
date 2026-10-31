package com.tiendatecnologica.service;

import com.tiendatecnologica.model.OrdenCompra;
import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.ProductoVendido;
import com.tiendatecnologica.model.Venta;
import com.tiendatecnologica.repository.ReporteRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Genera los reportes que pide el enunciado (ventas, más vendidos, bajo
 * inventario, compras y ganancias estimadas). Es una capa de solo lectura:
 * no persiste nada. Los cálculos los hace MySQL mediante
 * {@link ReporteRepository}; este servicio solo expone los resultados a los
 * controladores.
 *
 * Los reportes por rango de fechas reciben "desde" y "hasta" opcionales (null
 * significa sin límite). Los métodos pueden tardar (consultan la base de datos),
 * por eso {@code ReporteController} los invoca desde un {@code Task} de JavaFX
 * y no desde el hilo de la interfaz.
 */
public class ReporteService {

    private final ReporteRepository reporteRepository = new ReporteRepository();

    /**
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return las ventas (completadas y anuladas) hechas dentro de ese rango de fechas
     */
    public List<Venta> obtenerVentas(LocalDate desde, LocalDate hasta) {
        return reporteRepository.obtenerVentas(desde, hasta);
    }

    /**
     * Cantidad total de unidades vendidas por producto en ventas COMPLETADAS
     * (las anuladas no suman) dentro del rango, ordenado de mayor a menor.
     *
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return productos con su cantidad vendida en ese rango, de mayor a menor
     */
    public List<ProductoVendido> obtenerProductosMasVendidos(LocalDate desde, LocalDate hasta) {
        return reporteRepository.obtenerProductosMasVendidos(desde, hasta);
    }

    /**
     * Este reporte muestra el stock de hoy, por eso no recibe fechas.
     *
     * @return productos cuyo stock está en o por debajo del mínimo (incluye agotados, sin duplicados)
     */
    public List<Producto> obtenerProductosBajoInventario() {
        return reporteRepository.obtenerProductosBajoInventario();
    }

    /**
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return las órdenes de compra con fecha dentro de ese rango, con sus detalles
     */
    public List<OrdenCompra> obtenerCompras(LocalDate desde, LocalDate hasta) {
        return reporteRepository.obtenerCompras(desde, hasta);
    }

    /**
     * Calcula la ganancia estimada de las ventas COMPLETADAS dentro del rango,
     * como la diferencia entre el precio al que se vendió cada línea (congelado
     * en el detalle de venta) y el precio de compra ACTUAL del producto. Es una
     * estimación: si el precio de compra de un producto cambió después de
     * venderlo, no se puede saber cuál era el costo exacto en ese momento.
     *
     * @param desde primer día incluido, o null para no limitar por el inicio
     * @param hasta último día incluido, o null para no limitar por el final
     * @return la ganancia estimada de ese rango, redondeada a 2 decimales
     */
    public double calcularGananciasEstimadas(LocalDate desde, LocalDate hasta) {
        return reporteRepository.calcularGananciaEstimada(desde, hasta);
    }
}
