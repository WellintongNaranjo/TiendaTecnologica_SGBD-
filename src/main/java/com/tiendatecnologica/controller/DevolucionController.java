package com.tiendatecnologica.controller;

import com.tiendatecnologica.enums.EstadoDevolucion;
import com.tiendatecnologica.model.DetalleVenta;
import com.tiendatecnologica.model.Devolucion;
import com.tiendatecnologica.service.DevolucionService;
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
 * Controlador de la pantalla de gestión de devoluciones.
 *
 * Permite buscar una venta, seleccionar uno de sus productos,
 * registrar una solicitud de devolución y gestionar el estado
 * de las devoluciones registradas.
 *
 * La lógica de negocio se delega al {@link DevolucionService}.
 * El controlador se encarga de conectar la interfaz gráfica
 * con los servicios correspondientes.
 */
public class DevolucionController {

    /** Servicio encargado de gestionar las devoluciones. */
    private final DevolucionService devolucionService =
            new DevolucionService();

    /** Servicio utilizado para consultar las ventas y sus detalles. */
    private final VentaService ventaService =
            new VentaService();

    /**
     * Lista observable que contiene las devoluciones mostradas
     * en la tabla de historial.
     */
    private final ObservableList<Devolucion> devoluciones =
            FXCollections.observableArrayList();

    /** Campo para ingresar el identificador de la venta. */
    @FXML
    private TextField campoIdVenta;

    /** ComboBox que muestra los productos incluidos en la venta buscada. */
    @FXML
    private ComboBox<DetalleVenta> comboProductosVenta;

    /** Campo para ingresar la cantidad que se desea devolver. */
    @FXML
    private TextField campoCantidad;

    /** Campo para ingresar el motivo de la devolución. */
    @FXML
    private TextArea campoMotivo;

    /** Filtro por estado: "Todos" o uno de los valores de {@link EstadoDevolucion}. */
    @FXML
    private ComboBox<String> comboFiltroEstado;

    /** Última carga de la tabla en curso; solo se usa desde el hilo de JavaFX. */
    private Task<List<Devolucion>> tareaCarga;

    /** Ejecuta en segundo plano las operaciones que escriben (o consultan una venta) en MySQL. */
    private final EjecutorEnSegundoPlano ejecutor = new EjecutorEnSegundoPlano();

    /** Tabla que muestra el historial de devoluciones. */
    @FXML
    private TableView<Devolucion> tablaDevoluciones;

    /** Columna que muestra el identificador de la devolución. */
    @FXML
    private TableColumn<Devolucion, String> colDevolucionId;

    /** Columna que muestra el identificador de la venta. */
    @FXML
    private TableColumn<Devolucion, String> colDevolucionVenta;

    /** Columna que muestra el código del producto devuelto. */
    @FXML
    private TableColumn<Devolucion, String> colDevolucionProducto;

    /** Columna que muestra la cantidad solicitada para devolución. */
    @FXML
    private TableColumn<Devolucion, Number> colDevolucionCantidad;

    /** Columna que muestra la fecha de solicitud. */
    @FXML
    private TableColumn<Devolucion, String> colDevolucionFecha;

    /** Columna que muestra el estado actual de la devolución. */
    @FXML
    private TableColumn<Devolucion, String> colDevolucionEstado;

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de la tabla, establece el formato
     * utilizado para mostrar los productos de una venta y carga
     * las devoluciones registradas.
     */
    @FXML
    public void initialize() {

        colDevolucionId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colDevolucionVenta.setCellValueFactory(
                new PropertyValueFactory<>("idVenta")
        );

        colDevolucionProducto.setCellValueFactory(
                new PropertyValueFactory<>("codigoProducto")
        );

        colDevolucionCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad")
        );

        colDevolucionFecha.setCellValueFactory(
                new PropertyValueFactory<>("fechaSolicitud")
        );

        colDevolucionEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado")
        );

        tablaDevoluciones.setItems(devoluciones);

        FiltroEstado.configurar(comboFiltroEstado, EstadoDevolucion.class, this::cargarDevoluciones);

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
                                + " (compró "
                                + item.getCantidad()
                                + ")"
                );
            }
        });

        comboProductosVenta.setButtonCell(
                comboProductosVenta.getCellFactory().call(null)
        );

        cargarDevoluciones();
    }

    /**
     * Busca una venta utilizando el identificador ingresado.
     *
     * Si la venta existe, carga sus productos en el ComboBox
     * para que el usuario pueda seleccionar cuál desea devolver.
     */
    @FXML
    public void buscarVenta() {

        String idVenta = campoIdVenta.getText();

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
     * Registra una nueva solicitud de devolución.
     *
     * Utiliza la venta y el producto seleccionados, junto con
     * la cantidad y el motivo indicados por el usuario.
     */
    @FXML
    public void solicitarDevolucion() {

        DetalleVenta seleccionado =
                comboProductosVenta.getValue();

        if (seleccionado == null) {
            mostrarError(
                    "Busque una venta y seleccione un producto"
            );
            return;
        }

        int cantidad;

        try {
            cantidad = Integer.parseInt(campoCantidad.getText());
        } catch (NumberFormatException e) {
            mostrarError(
                    "Indique una cantidad entera válida"
            );
            return;
        }

        // Los valores de la pantalla se leen aquí, en el hilo de JavaFX.
        String idVenta = campoIdVenta.getText();
        String codigoProducto = seleccionado.getCodigoProducto();
        String motivo = campoMotivo.getText();

        ejecutor.ejecutar(
                () -> devolucionService.registrarDevolucion(
                        idVenta, codigoProducto, cantidad, motivo),
                registrada -> {
                    campoCantidad.clear();
                    campoMotivo.clear();

                    cargarDevoluciones();

                    mostrarInfo(
                            "Solicitud de devolución registrada"
                    );
                },
                this::mostrarError
        );
    }

    /**
     * Cambia la devolución seleccionada al estado EN_REVISION.
     */
    @FXML
    public void enviarARevision() {
        avanzarSeleccionada(EstadoDevolucion.EN_REVISION);
    }

    /**
     * Cambia la devolución seleccionada al estado APROBADA.
     *
     * Al aprobarse, el servicio se encarga de realizar
     * el reintegro correspondiente al inventario.
     */
    @FXML
    public void aprobarDevolucion() {
        avanzarSeleccionada(EstadoDevolucion.APROBADA);
    }

    /**
     * Cambia la devolución seleccionada al estado RECHAZADA.
     */
    @FXML
    public void rechazarDevolucion() {
        avanzarSeleccionada(EstadoDevolucion.RECHAZADA);
    }

    /**
     * Cambia la devolución seleccionada al estado FINALIZADA.
     */
    @FXML
    public void finalizarDevolucion() {
        avanzarSeleccionada(EstadoDevolucion.FINALIZADA);
    }

    /**
     * Elimina la devolución seleccionada, previa confirmación del usuario.
     *
     * El servicio solo permite eliminar devoluciones PENDIENTES; si no lo es,
     * se muestra el motivo y no se borra nada.
     */
    @FXML
    public void eliminarDevolucion() {

        Devolucion seleccionada =
                tablaDevoluciones
                        .getSelectionModel()
                        .getSelectedItem();

        if (seleccionada == null) {
            mostrarError(
                    "Seleccione una devolución de la tabla"
            );
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Eliminar la devolución " + seleccionada.getId()
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
                    devolucionService.eliminarDevolucion(id);
                    return null;
                },
                resultado -> cargarDevoluciones(),
                this::mostrarError
        );
    }

    /**
     * Recarga la tabla de devoluciones desde MySQL aplicando el filtro por
     * estado elegido ("Todos" muestra todas).
     *
     * La consulta corre en un {@link Task} para no congelar la interfaz; la
     * tabla se actualiza en {@code setOnSucceeded}, que ya se ejecuta en el
     * hilo de JavaFX. Si se pide una recarga nueva mientras otra sigue en
     * curso, la anterior se cancela y su resultado se descarta.
     */
    @FXML
    public void cargarDevoluciones() {

        EstadoDevolucion filtro =
                FiltroEstado.obtener(comboFiltroEstado, EstadoDevolucion.class);

        if (tareaCarga != null) {
            tareaCarga.cancel();
        }

        Task<List<Devolucion>> tarea = new Task<>() {
            @Override
            protected List<Devolucion> call() {
                return devolucionService.listarDevolucionesPorEstado(filtro);
            }
        };

        tarea.setOnSucceeded(evento ->
                devoluciones.setAll(tarea.getValue())
        );

        tarea.setOnFailed(evento -> {
            Throwable error = tarea.getException();
            mostrarError(error == null || error.getMessage() == null
                    ? "No se pudieron cargar las devoluciones"
                    : error.getMessage());
        });

        tareaCarga = tarea;

        Thread hilo = new Thread(tarea, "carga-devoluciones");
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Cambia el estado de la devolución seleccionada.
     *
     * El servicio valida si la transición de estado solicitada
     * es válida antes de realizar el cambio.
     *
     * @param nuevoEstado nuevo estado al que se desea avanzar
     */
    private void avanzarSeleccionada(
            EstadoDevolucion nuevoEstado) {

        Devolucion seleccionada =
                tablaDevoluciones
                        .getSelectionModel()
                        .getSelectedItem();

        if (seleccionada == null) {
            mostrarError(
                    "Seleccione una devolución de la tabla"
            );
            return;
        }

        String id = seleccionada.getId();

        ejecutor.ejecutar(
                () -> {
                    devolucionService.avanzarEstado(id, nuevoEstado);
                    return null;
                },
                resultado -> cargarDevoluciones(),
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