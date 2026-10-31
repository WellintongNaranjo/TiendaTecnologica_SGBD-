package com.tiendatecnologica.controller;

import com.tiendatecnologica.model.MovimientoInventario;
import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.service.InventarioService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la pantalla de gestión de inventario.
 *
 * Permite registrar entradas, salidas y ajustes de inventario,
 * consultar el historial de movimientos, filtrar movimientos por
 * producto y visualizar alertas de productos con bajo stock o
 * agotados.
 *
 * La lógica de negocio se delega al {@link InventarioService}.
 * El controlador se encarga de conectar la interfaz gráfica
 * con el servicio.
 */
public class InventarioController {

    /** Servicio encargado de gestionar los movimientos de inventario. */
    private final InventarioService inventarioService =
            new InventarioService();

    /** Última carga de alertas en curso; solo se usa desde el hilo de JavaFX. */
    private Task<List<Producto>> tareaAlertas;

    /** Campo para ingresar el código del producto. */
    @FXML
    private TextField campoCodigoProducto;

    /** Campo para ingresar la cantidad del movimiento. */
    @FXML
    private TextField campoCantidad;

    /** Campo para ingresar el motivo del movimiento. */
    @FXML
    private TextField campoMotivo;

    /** Tabla que muestra el historial de movimientos de inventario. */
    @FXML
    private TableView<MovimientoInventario> tablaMovimientos;

    /** Columna que muestra el identificador del movimiento. */
    @FXML
    private TableColumn<MovimientoInventario, String> colId;

    /** Columna que muestra el código del producto. */
    @FXML
    private TableColumn<MovimientoInventario, String> colProducto;

    /** Columna que muestra el tipo de movimiento. */
    @FXML
    private TableColumn<MovimientoInventario, String> colTipo;

    /** Columna que muestra la cantidad del movimiento. */
    @FXML
    private TableColumn<MovimientoInventario, Number> colCantidad;

    /** Columna que muestra la fecha del movimiento. */
    @FXML
    private TableColumn<MovimientoInventario, String> colFecha;

    /** Columna que muestra el motivo del movimiento. */
    @FXML
    private TableColumn<MovimientoInventario, String> colMotivo;

    /** Tabla que muestra los productos con alertas de stock. */
    @FXML
    private TableView<Producto> tablaAlertas;

    /** Columna que muestra el código del producto en las alertas. */
    @FXML
    private TableColumn<Producto, String> colAlertaCodigo;

    /** Columna que muestra el nombre del producto en las alertas. */
    @FXML
    private TableColumn<Producto, String> colAlertaNombre;

    /** Columna que muestra la cantidad disponible del producto. */
    @FXML
    private TableColumn<Producto, Number> colAlertaStock;

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de las tablas y carga el historial
     * de movimientos junto con las alertas de stock.
     */
    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colProducto.setCellValueFactory(
                new PropertyValueFactory<>("codigoProducto")
        );

        colTipo.setCellValueFactory(
                new PropertyValueFactory<>("tipo")
        );

        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad")
        );

        colFecha.setCellValueFactory(
                new PropertyValueFactory<>("fecha")
        );

        colMotivo.setCellValueFactory(
                new PropertyValueFactory<>("motivo")
        );

        colAlertaCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigo")
        );

        colAlertaNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        colAlertaStock.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDisponible")
        );

        cargarMovimientos();
        cargarAlertas();
    }

    /**
     * Registra una entrada de inventario.
     *
     * La cantidad ingresada se suma a la existencia actual
     * del producto. Las validaciones y la actualización del
     * inventario son realizadas por el servicio.
     */
    @FXML
    public void registrarEntrada() {
        try {
            inventarioService.registrarEntrada(
                    campoCodigoProducto.getText(),
                    Integer.parseInt(campoCantidad.getText()),
                    campoMotivo.getText()
            );
            despuesDeMovimiento();

        } catch (NumberFormatException e) {
            mostrarError("La cantidad debe ser un número entero válido");

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    /**
     * Registra una salida de inventario.
     *
     * La cantidad ingresada se resta de la existencia actual
     * del producto. El servicio se encarga de validar que exista
     * suficiente inventario.
     */
    @FXML
    public void registrarSalida() {

        try {

            inventarioService.registrarSalida(
                    campoCodigoProducto.getText(),
                    Integer.parseInt(campoCantidad.getText()),
                    campoMotivo.getText()
            );

            despuesDeMovimiento();

        } catch (NumberFormatException e) {

            mostrarError("La cantidad debe ser un número entero válido");

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Realiza un ajuste de inventario.
     *
     * La cantidad ingresada representa la existencia final que
     * debe tener el producto después del ajuste.
     */
    @FXML
    public void realizarAjuste() {

        try {

            inventarioService.ajustarInventario(
                    campoCodigoProducto.getText(),
                    Integer.parseInt(campoCantidad.getText()),
                    campoMotivo.getText()
            );

            despuesDeMovimiento();

        } catch (NumberFormatException e) {

            mostrarError("La cantidad debe ser un número entero válido");

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Carga todos los movimientos de inventario registrados.
     */
    @FXML
    public void cargarMovimientos() {

        ObservableList<MovimientoInventario> movimientos =
                FXCollections.observableArrayList(
                        inventarioService.obtenerMovimientos()
                );

        tablaMovimientos.setItems(movimientos);
    }

    /**
     * Carga los productos que presentan alertas de stock.
     *
     * Se incluyen tanto los productos con bajo stock como
     * aquellos cuya existencia se encuentra agotada.
     *
     * Las consultas se hacen en un {@link Task} (fuera del hilo de JavaFX)
     * para que la ventana no se congele; la tabla se actualiza en
     * {@code setOnSucceeded}, que ya corre en el hilo de JavaFX. Si se pide
     * una recarga nueva mientras otra sigue en curso, la anterior se cancela
     * y su resultado se descarta.
     */
    @FXML
    public void cargarAlertas() {

        if (tareaAlertas != null) {
            tareaAlertas.cancel();
        }

        Task<List<Producto>> tarea = new Task<>() {
            @Override
            protected List<Producto> call() {
                // Solo consulta datos; no toca ningún control de JavaFX.
                List<Producto> alertas =
                        new ArrayList<>(inventarioService.obtenerProductosBajoStock());
                alertas.addAll(inventarioService.obtenerProductosAgotados());
                return alertas;
            }
        };

        tarea.setOnSucceeded(evento ->
                tablaAlertas.setItems(
                        FXCollections.observableArrayList(tarea.getValue())
                )
        );

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            mostrarError(error == null || error.getMessage() == null
                    ? "No se pudieron cargar las alertas de stock"
                    : error.getMessage());
        });

        tareaAlertas = tarea;

        Thread hilo = new Thread(tarea, "carga-alertas");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Filtra el historial de movimientos por código de producto.
     *
     * Si el campo de código está vacío, se muestran nuevamente
     * todos los movimientos.
     */
    @FXML
    public void filtrarMovimientos() {

        String codigo =
                campoCodigoProducto.getText();

        if (codigo == null || codigo.isBlank()) {
            cargarMovimientos();
            return;
        }

        ObservableList<MovimientoInventario> movimientos =
                FXCollections.observableArrayList(
                        inventarioService.obtenerMovimientosProducto(codigo)
                );

        tablaMovimientos.setItems(movimientos);
    }

    /**
     * Actualiza la información de la pantalla después de registrar
     * un movimiento de inventario.
     *
     * Recarga el historial, actualiza las alertas y limpia los
     * campos de cantidad y motivo.
     */
    private void despuesDeMovimiento() {

        cargarMovimientos();
        cargarAlertas();

        campoCantidad.clear();
        campoMotivo.clear();
    }

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param mensaje mensaje que se mostrará
     */
    private void mostrarError(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.ERROR,
                mensaje
        );

        alerta.setHeaderText(
                "No se pudo registrar el movimiento"
        );

        alerta.showAndWait();
    }
}