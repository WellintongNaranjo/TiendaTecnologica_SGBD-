package com.tiendatecnologica.controller;

import com.tiendatecnologica.model.OrdenCompra;
import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.ProductoVendido;
import com.tiendatecnologica.model.Venta;
import com.tiendatecnologica.service.ReporteService;
import com.tiendatecnologica.util.ExportadorCsv;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador de la pantalla de reportes.
 *
 * Es de solo lectura: obtiene la información calculada por
 * {@link ReporteService} y la muestra en las diferentes tablas
 * y etiquetas de la interfaz. Las consultas a MySQL se ejecutan en un
 * {@link Task} para que la ventana no se congele, y cada tabla se puede
 * exportar a un archivo CSV.
 *
 * Los reportes incluyen información sobre ventas, productos más
 * vendidos, productos con bajo inventario, compras a proveedores
 * y ganancias estimadas.
 */
public class ReporteController {

    /** Servicio encargado de generar la información de los reportes. */
    private final ReporteService reporteService =
            new ReporteService();

    /** Formato de fecha y hora que se escribe en los CSV. */
    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Resultado completo de una carga de reportes. Se arma en el hilo de
     * fondo y se entrega a la interfaz ya terminado.
     */
    private record DatosReportes(List<Venta> ventas,
                                 List<ProductoVendido> masVendidos,
                                 List<Producto> bajoInventario,
                                 List<OrdenCompra> compras,
                                 double ganancias) {
    }

    /** Carga de reportes en curso, o null; solo se usa desde el hilo de JavaFX. */
    private Task<DatosReportes> tareaCarga;

    /** Carpeta donde el usuario guardó el último CSV, para recordarla. */
    private File ultimaCarpetaExportacion;

    /** Botón que recarga los reportes; se deshabilita mientras se cargan. */
    @FXML
    private Button botonActualizar;

    /** Botón que aplica el filtro de fechas; se deshabilita mientras se cargan los reportes. */
    @FXML
    private Button botonFiltrar;

    /** Botón que quita el filtro de fechas; se deshabilita mientras se cargan los reportes. */
    @FXML
    private Button botonLimpiarFiltro;

    /** Fecha inicial del filtro (vacía = sin límite). */
    @FXML
    private DatePicker fechaDesde;

    /** Fecha final del filtro, incluida (vacía = sin límite). */
    @FXML
    private DatePicker fechaHasta;

    /** Etiqueta con el estado de la carga ("Cargando reportes..."). */
    @FXML
    private Label labelEstadoReportes;

    // --- Ventas ---

    /** Tabla que muestra las ventas registradas. */
    @FXML
    private TableView<Venta> tablaVentas;

    /** Columna que muestra el identificador de la venta. */
    @FXML
    private TableColumn<Venta, String> colVentaId;

    /** Columna que muestra la fecha de la venta. */
    @FXML
    private TableColumn<Venta, String> colVentaFecha;

    /** Columna que muestra el identificador del cliente. */
    @FXML
    private TableColumn<Venta, String> colVentaCliente;

    /** Columna que muestra el total de la venta. */
    @FXML
    private TableColumn<Venta, Number> colVentaTotal;

    /** Columna que muestra el estado de la venta. */
    @FXML
    private TableColumn<Venta, String> colVentaEstado;

    // --- Productos más vendidos ---

    /** Tabla que muestra los productos con mayor cantidad de ventas. */
    @FXML
    private TableView<ProductoVendido> tablaMasVendidos;

    /** Columna que muestra el código del producto más vendido. */
    @FXML
    private TableColumn<ProductoVendido, String> colMasVendidoCodigo;

    /** Columna que muestra el nombre del producto más vendido. */
    @FXML
    private TableColumn<ProductoVendido, String> colMasVendidoNombre;

    /** Columna que muestra la cantidad vendida del producto. */
    @FXML
    private TableColumn<ProductoVendido, Number> colMasVendidoCantidad;

    // --- Bajo inventario ---

    /** Tabla que muestra los productos con bajo inventario. */
    @FXML
    private TableView<Producto> tablaBajoInventario;

    /** Columna que muestra el código del producto. */
    @FXML
    private TableColumn<Producto, String> colBajoInvCodigo;

    /** Columna que muestra el nombre del producto. */
    @FXML
    private TableColumn<Producto, String> colBajoInvNombre;

    /** Columna que muestra la cantidad disponible del producto. */
    @FXML
    private TableColumn<Producto, Number> colBajoInvDisponible;

    /** Columna que muestra el stock mínimo configurado. */
    @FXML
    private TableColumn<Producto, Number> colBajoInvMinimo;

    // --- Compras a proveedores ---

    /** Tabla que muestra las órdenes de compra registradas. */
    @FXML
    private TableView<OrdenCompra> tablaCompras;

    /** Columna que muestra el identificador de la orden de compra. */
    @FXML
    private TableColumn<OrdenCompra, String> colCompraId;

    /** Columna que muestra el identificador del proveedor. */
    @FXML
    private TableColumn<OrdenCompra, String> colCompraProveedor;

    /** Columna que muestra la fecha de la orden de compra. */
    @FXML
    private TableColumn<OrdenCompra, String> colCompraFecha;

    /** Columna que muestra el estado de la orden de compra. */
    @FXML
    private TableColumn<OrdenCompra, String> colCompraEstado;

    /** Columna que muestra el total de la orden de compra. */
    @FXML
    private TableColumn<OrdenCompra, Double> colCompraTotal;

    // --- Ganancias estimadas ---

    /** Etiqueta donde se muestra la ganancia estimada. */
    @FXML
    private Label labelGananciasEstimadas;

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de las diferentes tablas y carga
     * todos los reportes disponibles.
     */
    @FXML
    public void initialize() {

        colVentaId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colVentaFecha.setCellValueFactory(
                new PropertyValueFactory<>("fecha")
        );

        colVentaCliente.setCellValueFactory(
                new PropertyValueFactory<>("idCliente")
        );

        colVentaTotal.setCellValueFactory(
                new PropertyValueFactory<>("total")
        );

        colVentaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado")
        );

        colMasVendidoCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigoProducto")
        );

        colMasVendidoNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombreProducto")
        );

        colMasVendidoCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidadVendida")
        );

        colBajoInvCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigo")
        );

        colBajoInvNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        colBajoInvDisponible.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDisponible")
        );

        colBajoInvMinimo.setCellValueFactory(
                new PropertyValueFactory<>("stockMinimo")
        );

        colCompraId.setCellValueFactory(
                new PropertyValueFactory<>("idOrden")
        );

        colCompraProveedor.setCellValueFactory(
                new PropertyValueFactory<>("idProveedor")
        );

        colCompraFecha.setCellValueFactory(
                new PropertyValueFactory<>("fecha")
        );

        colCompraEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado")
        );

        /*
         * OrdenCompra no tiene un método getTotal().
         * El total se obtiene mediante calcularTotal(), por lo que
         * esta columna debe configurarse manualmente.
         */
        colCompraTotal.setCellValueFactory(
                datos -> new SimpleDoubleProperty(
                        datos.getValue().calcularTotal()
                ).asObject()
        );

        cargarReportes();
    }

    /**
     * Recarga todos los reportes mostrados en la pantalla.
     *
     * Aplica el rango de fechas elegido ("Desde" y "Hasta", ambos opcionales) a
     * ventas, más vendidos, compras y ganancia. Bajo inventario no usa fechas
     * porque muestra el stock de hoy.
     *
     * Las consultas se hacen en un {@link Task} (fuera del hilo de JavaFX)
     * y las tablas y la ganancia estimada se actualizan en
     * {@code setOnSucceeded}, que ya corre en el hilo de JavaFX. Si ya hay
     * una carga en curso no se lanza otra. Si ocurre un error, se muestra
     * un mensaje y las tablas conservan lo que tenían.
     */
    @FXML
    public void cargarReportes() {

        if (tareaCarga != null && tareaCarga.isRunning()) {
            return;
        }

        // Los controles se leen aquí, en el hilo de JavaFX; la tarea solo recibe los valores.
        final LocalDate desde = fechaDesde.getValue();
        final LocalDate hasta = fechaHasta.getValue();

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            mostrarError("La fecha \"Desde\" no puede ser posterior a la fecha \"Hasta\".");
            return;
        }

        Task<DatosReportes> tarea = new Task<>() {
            @Override
            protected DatosReportes call() {
                // Solo consulta datos; no toca ningún control de JavaFX.
                return new DatosReportes(
                        reporteService.obtenerVentas(desde, hasta),
                        reporteService.obtenerProductosMasVendidos(desde, hasta),
                        reporteService.obtenerProductosBajoInventario(),
                        reporteService.obtenerCompras(desde, hasta),
                        reporteService.calcularGananciasEstimadas(desde, hasta)
                );
            }
        };

        tarea.setOnSucceeded(evento -> {
            DatosReportes datos = tarea.getValue();

            tablaVentas.setItems(
                    FXCollections.observableArrayList(datos.ventas())
            );
            tablaMasVendidos.setItems(
                    FXCollections.observableArrayList(datos.masVendidos())
            );
            tablaBajoInventario.setItems(
                    FXCollections.observableArrayList(datos.bajoInventario())
            );
            tablaCompras.setItems(
                    FXCollections.observableArrayList(datos.compras())
            );
            labelGananciasEstimadas.setText(
                    String.format(Locale.US, "%.2f", datos.ganancias())
            );

            terminarCarga("");
        });

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            terminarCarga("No se pudieron cargar los reportes");
            mostrarError(
                    error == null || error.getMessage() == null
                            ? "Ocurrió un error inesperado al consultar los reportes"
                            : error.getMessage()
            );
        });

        tareaCarga = tarea;
        habilitarControles(false);
        labelEstadoReportes.setText("Cargando reportes...");

        Thread hilo = new Thread(tarea, "carga-reportes");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Deja la pantalla lista después de una carga (exitosa o fallida).
     *
     * @param mensaje texto a mostrar en la etiqueta de estado (vacío si todo salió bien)
     */
    private void terminarCarga(String mensaje) {
        habilitarControles(true);
        labelEstadoReportes.setText(mensaje);
    }

    /**
     * Habilita o deshabilita los botones de carga para no lanzar dos cargas a la vez.
     *
     * @param habilitados true para permitir usarlos, false mientras se consulta
     */
    private void habilitarControles(boolean habilitados) {
        botonActualizar.setDisable(!habilitados);
        botonFiltrar.setDisable(!habilitados);
        botonLimpiarFiltro.setDisable(!habilitados);
    }

    /**
     * Quita el filtro de fechas y vuelve a cargar los reportes con todos los datos.
     */
    @FXML
    public void limpiarFiltroFechas() {
        fechaDesde.setValue(null);
        fechaHasta.setValue(null);
        cargarReportes();
    }

    // ------------------------------------------------------------------
    // Exportación a CSV
    // ------------------------------------------------------------------

    /** Exporta a CSV las ventas que muestra la tabla. */
    @FXML
    public void exportarVentas() {
        List<String[]> filas = new ArrayList<>();
        for (Venta v : tablaVentas.getItems()) {
            filas.add(new String[] {
                    v.getId(),
                    v.getFecha() == null ? "" : v.getFecha().format(FORMATO_FECHA_HORA),
                    v.getIdCliente(),
                    dinero(v.getTotal()),
                    v.getEstado().name()
            });
        }
        exportarCsv("ventas", new String[] {"Id", "Fecha", "Cliente", "Total", "Estado"}, filas);
    }

    /** Exporta a CSV los productos más vendidos que muestra la tabla. */
    @FXML
    public void exportarMasVendidos() {
        List<String[]> filas = new ArrayList<>();
        for (ProductoVendido p : tablaMasVendidos.getItems()) {
            filas.add(new String[] {
                    p.getCodigoProducto(),
                    p.getNombreProducto(),
                    String.valueOf(p.getCantidadVendida())
            });
        }
        exportarCsv("productos_mas_vendidos",
                new String[] {"Código", "Producto", "Unidades vendidas"}, filas);
    }

    /** Exporta a CSV los productos con bajo inventario que muestra la tabla. */
    @FXML
    public void exportarBajoInventario() {
        List<String[]> filas = new ArrayList<>();
        for (Producto p : tablaBajoInventario.getItems()) {
            filas.add(new String[] {
                    p.getCodigo(),
                    p.getNombre(),
                    String.valueOf(p.getCantidadDisponible()),
                    String.valueOf(p.getStockMinimo())
            });
        }
        exportarCsv("bajo_inventario",
                new String[] {"Código", "Producto", "Disponible", "Mínimo"}, filas);
    }

    /** Exporta a CSV las órdenes de compra que muestra la tabla. */
    @FXML
    public void exportarCompras() {
        List<String[]> filas = new ArrayList<>();
        for (OrdenCompra o : tablaCompras.getItems()) {
            filas.add(new String[] {
                    o.getIdOrden(),
                    o.getIdProveedor(),
                    o.getFecha(),
                    o.getEstado(),
                    dinero(o.calcularTotal())
            });
        }
        exportarCsv("compras",
                new String[] {"Orden", "Proveedor", "Fecha", "Estado", "Total"}, filas);
    }

    /**
     * Pide al usuario dónde guardar el archivo y lo escribe en un
     * {@link Task}, para no bloquear la interfaz. Si el usuario cancela el
     * selector no pasa nada; si falla la escritura se muestra el error.
     *
     * @param nombreBase  parte inicial del nombre sugerido (se le agrega la fecha)
     * @param encabezados encabezados de las columnas del CSV
     * @param filas       datos ya convertidos a texto (copia de lo que muestra la tabla)
     */
    private void exportarCsv(String nombreBase, String[] encabezados, List<String[]> filas) {

        if (filas.isEmpty()) {
            mostrarInfo("No hay datos para exportar en este reporte.");
            return;
        }

        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar reporte como CSV");
        selector.setInitialFileName(nombreBase + "_" + LocalDate.now() + ".csv");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv")
        );
        if (ultimaCarpetaExportacion != null && ultimaCarpetaExportacion.isDirectory()) {
            selector.setInitialDirectory(ultimaCarpetaExportacion);
        }

        Window ventana = tablaVentas.getScene() == null ? null : tablaVentas.getScene().getWindow();
        File elegido = selector.showSaveDialog(ventana);

        if (elegido == null) {
            return;   // el usuario canceló el selector
        }

        Path destino = elegido.toPath();
        if (!destino.getFileName().toString().toLowerCase().endsWith(".csv")) {
            destino = destino.resolveSibling(destino.getFileName() + ".csv");
        }
        final Path rutaFinal = destino;
        ultimaCarpetaExportacion = rutaFinal.toAbsolutePath().getParent().toFile();

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() throws Exception {
                ExportadorCsv.escribir(rutaFinal, encabezados, filas);
                return null;
            }
        };

        tarea.setOnSucceeded(evento ->
                mostrarInfo("Reporte exportado en:\n" + rutaFinal)
        );

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            mostrarError("No se pudo exportar el reporte. "
                    + (error == null || error.getMessage() == null ? "" : error.getMessage()));
        });

        Thread hilo = new Thread(tarea, "exportar-csv");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Formatea un monto con dos decimales y punto decimal (sin separador de miles).
     *
     * @param valor monto a formatear
     * @return el monto como texto, por ejemplo "519235.00"
     */
    private static String dinero(double valor) {
        return String.format(Locale.US, "%.2f", valor);
    }

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param mensaje mensaje que se mostrará
     */
    private void mostrarError(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR, mensaje);
        alerta.setHeaderText("No se pudo completar la operación");
        alerta.showAndWait();
    }

    /**
     * Muestra un mensaje informativo al usuario.
     *
     * @param mensaje mensaje que se mostrará
     */
    private void mostrarInfo(String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, mensaje);
        alerta.setHeaderText("Listo");
        alerta.showAndWait();
    }
}