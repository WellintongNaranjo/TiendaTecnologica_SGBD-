package com.tiendatecnologica.controller;

import com.tiendatecnologica.enums.EstadoGarantia;
import com.tiendatecnologica.model.DetalleVenta;
import com.tiendatecnologica.model.Garantia;
import com.tiendatecnologica.service.GarantiaService;
import com.tiendatecnologica.service.VentaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controlador de la pantalla de gestión de garantías.
 *
 * Permite buscar una venta, seleccionar uno de sus productos,
 * registrar una solicitud de garantía y gestionar el estado
 * de las garantías registradas.
 *
 * La lógica de negocio se delega al {@link GarantiaService}.
 * El controlador se encarga de conectar la interfaz gráfica
 * con los servicios correspondientes.
 */
public class GarantiaController {

    /** Servicio encargado de gestionar las garantías. */
    private final GarantiaService garantiaService =
            new GarantiaService();

    /** Servicio utilizado para consultar las ventas y sus detalles. */
    private final VentaService ventaService =
            new VentaService();

    /**
     * Lista observable que contiene las garantías mostradas
     * en la tabla de historial.
     */
    private final ObservableList<Garantia> garantias =
            FXCollections.observableArrayList();

    /** Campo para ingresar el identificador de la venta. */
    @FXML
    private TextField campoIdVenta;

    /** ComboBox que muestra los productos incluidos en la venta buscada. */
    @FXML
    private ComboBox<DetalleVenta> comboProductosVenta;

    /** Campo para describir el problema presentado por el producto. */
    @FXML
    private TextArea campoDescripcionProblema;

    /** Filtro por estado: "Todos" o uno de los valores de {@link EstadoGarantia}. */
    @FXML
    private ComboBox<String> comboFiltroEstado;

    /** Última carga de la tabla en curso; solo se usa desde el hilo de JavaFX. */
    private Task<List<Garantia>> tareaCarga;

    /** Ejecuta en segundo plano las operaciones que escriben (o consultan una venta) en MySQL. */
    private final EjecutorEnSegundoPlano ejecutor = new EjecutorEnSegundoPlano();

    /** Tabla que muestra el historial de garantías. */
    @FXML
    private TableView<Garantia> tablaGarantias;

    /** Columna que muestra el identificador de la garantía. */
    @FXML
    private TableColumn<Garantia, String> colGarantiaId;

    /** Columna que muestra el identificador de la venta. */
    @FXML
    private TableColumn<Garantia, String> colGarantiaVenta;

    /** Columna que muestra el código del producto. */
    @FXML
    private TableColumn<Garantia, String> colGarantiaProducto;

    /** Columna que muestra la fecha de solicitud. */
    @FXML
    private TableColumn<Garantia, String> colGarantiaFecha;

    /** Columna que muestra el estado actual de la garantía. */
    @FXML
    private TableColumn<Garantia, String> colGarantiaEstado;

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de la tabla, establece el formato
     * utilizado para mostrar los productos de una venta y carga
     * las garantías registradas.
     */
    @FXML
    public void initialize() {

        colGarantiaId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colGarantiaVenta.setCellValueFactory(
                new PropertyValueFactory<>("idVenta")
        );

        colGarantiaProducto.setCellValueFactory(
                new PropertyValueFactory<>("codigoProducto")
        );

        colGarantiaFecha.setCellValueFactory(
                new PropertyValueFactory<>("fechaSolicitud")
        );

        colGarantiaEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado")
        );

        tablaGarantias.setItems(garantias);

        FiltroEstado.configurar(comboFiltroEstado, EstadoGarantia.class, this::cargarGarantias);

        /*
         * Personaliza la forma en que los productos aparecen
         * dentro del ComboBox para facilitar su identificación.
         */
        comboProductosVenta.setCellFactory(param -> new ListCell<>() {

            @Override
            protected void updateItem(
                    DetalleVenta item,
                    boolean empty) {

                super.updateItem(item, empty);

                setText(
                        empty || item == null
                                ? null
                                : item.getCodigoProducto()
                                + " - "
                                + item.getNombreProducto()
                );
            }
        });

        comboProductosVenta.setButtonCell(
                comboProductosVenta.getCellFactory().call(null)
        );

        cargarGarantias();
    }

    /**
     * Busca una venta utilizando el identificador ingresado.
     *
     * Si la venta existe, carga sus productos en el ComboBox
     * para que el usuario pueda seleccionar cuál desea registrar
     * para garantía.
     */
    @FXML
    public void buscarVenta() {

        String idVenta =
                campoIdVenta.getText();

        if (idVenta == null || idVenta.isBlank()) {
            mostrarError("Indique el id de una venta");
            return;
        }

        // La consulta corre en segundo plano; devuelve null si la venta no existe.
        ejecutor.ejecutar(
                () -> ventaService.buscarVenta(idVenta) == null
                        ? null
                        : ventaService.obtenerDetalle(idVenta),
                detalles -> {
                    if (detalles == null) {
                        mostrarError("No existe la venta " + idVenta);
                        comboProductosVenta.setItems(
                                FXCollections.observableArrayList()
                        );
                    } else {
                        comboProductosVenta.setItems(
                                FXCollections.observableArrayList(detalles)
                        );
                    }
                },
                this::mostrarError
        );
    }

    /**
     * Registra una nueva solicitud de garantía.
     *
     * Utiliza la venta y el producto seleccionados junto con
     * la descripción del problema indicada por el usuario.
     */
    @FXML
    public void solicitarGarantia() {

        DetalleVenta seleccionado =
                comboProductosVenta.getValue();

        if (seleccionado == null) {
            mostrarError(
                    "Busque una venta y seleccione un producto"
            );
            return;
        }

        // Los valores de la pantalla se leen aquí, en el hilo de JavaFX.
        String idVenta = campoIdVenta.getText();
        String codigoProducto = seleccionado.getCodigoProducto();
        String descripcion = campoDescripcionProblema.getText();

        ejecutor.ejecutar(
                () -> garantiaService.registrarGarantia(
                        idVenta, codigoProducto, descripcion),
                registrada -> {
                    campoDescripcionProblema.clear();

                    cargarGarantias();

                    mostrarInfo(
                            "Solicitud de garantía registrada"
                    );
                },
                this::mostrarError
        );
    }

    /**
     * Cambia la garantía seleccionada al estado EN_REVISION.
     */
    @FXML
    public void enviarARevision() {
        avanzarSeleccionada(EstadoGarantia.EN_REVISION);
    }

    /**
     * Cambia la garantía seleccionada al estado APROBADA.
     */
    @FXML
    public void aprobarGarantia() {
        avanzarSeleccionada(EstadoGarantia.APROBADA);
    }

    /**
     * Cambia la garantía seleccionada al estado RECHAZADA.
     */
    @FXML
    public void rechazarGarantia() {
        avanzarSeleccionada(EstadoGarantia.RECHAZADA);
    }

    /**
     * Cambia la garantía seleccionada al estado FINALIZADA.
     */
    @FXML
    public void finalizarGarantia() {
        avanzarSeleccionada(EstadoGarantia.FINALIZADA);
    }

    /**
     * Elimina la garantía seleccionada, previa confirmación del usuario.
     *
     * El servicio solo permite eliminar garantías PENDIENTES; si no lo es,
     * se muestra el motivo y no se borra nada.
     */
    @FXML
    public void eliminarGarantia() {

        Garantia seleccionada =
                tablaGarantias
                        .getSelectionModel()
                        .getSelectedItem();

        if (seleccionada == null) {
            mostrarError(
                    "Seleccione una garantía de la tabla"
            );
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Eliminar la garantía " + seleccionada.getId()
                        + "? Esta acción no se puede deshacer.",
                ButtonType.YES,
                ButtonType.NO
        );
        confirmacion.setHeaderText("Confirmar eliminación");

        if (confirmacion.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) {
            return;
        }

        String id = seleccionada.getId();

        ejecutor.ejecutar(
                () -> {
                    garantiaService.eliminarGarantia(id);
                    return null;
                },
                resultado -> cargarGarantias(),
                this::mostrarError
        );
    }

    /**
     * Recarga la tabla de garantías desde MySQL aplicando el filtro por estado
     * elegido ("Todos" muestra todas).
     *
     * La consulta corre en un {@link Task} para no congelar la interfaz; la
     * tabla se actualiza en {@code setOnSucceeded}, que ya se ejecuta en el
     * hilo de JavaFX. Si se pide una recarga nueva mientras otra sigue en
     * curso, la anterior se cancela y su resultado se descarta.
     */
    @FXML
    public void cargarGarantias() {

        EstadoGarantia filtro =
                FiltroEstado.obtener(comboFiltroEstado, EstadoGarantia.class);

        if (tareaCarga != null) {
            tareaCarga.cancel();
        }

        Task<List<Garantia>> tarea = new Task<>() {
            @Override
            protected List<Garantia> call() {
                return garantiaService.listarGarantiasPorEstado(filtro);
            }
        };

        tarea.setOnSucceeded(evento ->
                garantias.setAll(tarea.getValue())
        );

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            mostrarError(error == null || error.getMessage() == null
                    ? "No se pudieron cargar las garantías"
                    : error.getMessage());
        });

        tareaCarga = tarea;

        Thread hilo = new Thread(tarea, "carga-garantias");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Cambia el estado de la garantía seleccionada.
     *
     * El servicio se encarga de validar si la transición
     * solicitada es válida.
     *
     * @param nuevoEstado nuevo estado al que se desea avanzar
     */
    private void avanzarSeleccionada(
            EstadoGarantia nuevoEstado) {

        Garantia seleccionada =
                tablaGarantias
                        .getSelectionModel()
                        .getSelectedItem();

        if (seleccionada == null) {
            mostrarError(
                    "Seleccione una garantía de la tabla"
            );
            return;
        }

        String id = seleccionada.getId();

        ejecutor.ejecutar(
                () -> {
                    garantiaService.avanzarEstado(id, nuevoEstado);
                    return null;
                },
                resultado -> cargarGarantias(),
                this::mostrarError
        );
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
                "No se pudo completar la operación"
        );

        alerta.showAndWait();
    }

    /**
     * Muestra un mensaje informativo al usuario.
     *
     * @param mensaje mensaje que se mostrará
     */
    private void mostrarInfo(String mensaje) {

        Alert alerta = new Alert(
                Alert.AlertType.INFORMATION,
                mensaje
        );

        alerta.setHeaderText("Listo");

        alerta.showAndWait();
    }
}