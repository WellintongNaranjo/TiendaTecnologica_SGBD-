package com.tiendatecnologica.controller;

import com.tiendatecnologica.model.DetalleCompra;
import com.tiendatecnologica.model.OrdenCompra;
import com.tiendatecnologica.service.CompraService;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador de la vista de gestión de compras.
 *
 * Permite registrar órdenes de compra, agregar sus detalles,
 * consultar las órdenes registradas y cambiar su estado a
 * Recibida o Cancelada.
 *
 * La lógica de negocio se delega al {@link CompraService}.
 */
public class CompraController {

    /** Campo para ingresar el identificador de la orden. */
    @FXML
    private TextField txtIdOrden;

    /** Campo para ingresar el identificador del proveedor. */
    @FXML
    private TextField txtIdProveedor;

    /** Campo para ingresar la fecha de la orden. */
    @FXML
    private TextField txtFecha;

    /** Campo para ingresar el identificador del producto. */
    @FXML
    private TextField txtIdProducto;

    /** Campo para ingresar la cantidad del producto. */
    @FXML
    private TextField txtCantidad;

    /** Campo para ingresar el costo unitario del producto. */
    @FXML
    private TextField txtCostoUnitario;

    /** Tabla que muestra las órdenes de compra registradas. */
    @FXML
    private TableView<OrdenCompra> tablaCompras;

    /** Columna que muestra el identificador de la orden. */
    @FXML
    private TableColumn<OrdenCompra, String> colIdOrden;

    /** Columna que muestra el identificador del proveedor. */
    @FXML
    private TableColumn<OrdenCompra, String> colIdProveedor;

    /** Columna que muestra la fecha de la orden. */
    @FXML
    private TableColumn<OrdenCompra, String> colFecha;

    /** Columna que muestra el estado de la orden. */
    @FXML
    private TableColumn<OrdenCompra, String> colEstado;

    /** Tabla que muestra los detalles de la orden que se está creando. */
    @FXML
    private TableView<DetalleCompra> tablaDetalles;

    /** Columna que muestra el identificador del producto. */
    @FXML
    private TableColumn<DetalleCompra, String> colIdProducto;

    /** Columna que muestra la cantidad solicitada. */
    @FXML
    private TableColumn<DetalleCompra, Integer> colCantidad;

    /** Columna que muestra el costo unitario. */
    @FXML
    private TableColumn<DetalleCompra, Double> colCostoUnitario;

    /** Columna que muestra el subtotal del detalle. */
    @FXML
    private TableColumn<DetalleCompra, Double> colSubtotal;

    /** Servicio encargado de gestionar las órdenes de compra. */
    private final CompraService servicio = new CompraService();

    /** Lista observable utilizada para mostrar las órdenes en la tabla. */
    private ObservableList<OrdenCompra> listaCompras;

    /** Ejecuta la carga de la tabla de órdenes en un hilo aparte. */
    private final EjecutorEnSegundoPlano ejecutorCarga = new EjecutorEnSegundoPlano();

    /** Ejecuta la recepción de una orden (varias escrituras) en un hilo aparte. */
    private final EjecutorEnSegundoPlano ejecutorRecepcion = new EjecutorEnSegundoPlano();

    /**
     * Lista temporal de detalles de la orden que se está creando.
     *
     * Los detalles se almacenan aquí antes de registrar la orden
     * completa mediante el servicio.
     */
    private final ObservableList<DetalleCompra> listaDetalles =
            FXCollections.observableArrayList();

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de las tablas, establece la lista
     * temporal de detalles y carga las órdenes almacenadas.
     */
    @FXML
    public void initialize() {

        colIdOrden.setCellValueFactory(
                new PropertyValueFactory<>("idOrden"));

        colIdProveedor.setCellValueFactory(
                new PropertyValueFactory<>("idProveedor"));

        colFecha.setCellValueFactory(
                new PropertyValueFactory<>("fecha"));

        colEstado.setCellValueFactory(
                new PropertyValueFactory<>("estado"));

        colIdProducto.setCellValueFactory(
                new PropertyValueFactory<>("idProducto"));

        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidad"));

        colCostoUnitario.setCellValueFactory(
                new PropertyValueFactory<>("costoUnitario"));

        /*
         * El subtotal no es un atributo almacenado directamente.
         * Se calcula a partir de la cantidad y el costo unitario
         * de cada detalle.
         */
        colSubtotal.setCellValueFactory(
                datos -> new SimpleObjectProperty<>(
                        datos.getValue().calcularSubtotal()
                )
        );

        tablaDetalles.setItems(listaDetalles);

        cargarDatos();
    }

    /**
     * Carga las órdenes de compra almacenadas.
     *
     * Obtiene las órdenes mediante el servicio y las coloca
     * en la tabla correspondiente.
     */
    private void cargarDatos() {

        // La consulta corre en un hilo aparte; si falla se muestra una alerta.
        ejecutorCarga.ejecutar(
                servicio::obtenerCompras,
                compras -> {
                    listaCompras = FXCollections.observableArrayList(compras);
                    tablaCompras.setItems(listaCompras);
                },
                this::mostrarError
        );
    }

    /**
     * Agrega un detalle a la orden que se está creando.
     *
     * El detalle permanece temporalmente en la lista hasta que
     * el usuario registra la orden completa. Valida que los
     * campos no estén vacíos, que la cantidad y el costo sean
     * numéricos, y que la cantidad sea mayor que 0 y el costo
     * no sea negativo.
     */
    @FXML
    public void agregarDetalle() {

        String idProducto =
                txtIdProducto.getText().trim();

        String cantidadTexto =
                txtCantidad.getText().trim();

        String costoTexto =
                txtCostoUnitario.getText().trim();

        if (idProducto.isEmpty()
                || cantidadTexto.isEmpty()
                || costoTexto.isEmpty()) {

            mostrarError(
                    "Debe completar todos los datos del producto."
            );

            return;
        }

        try {

            int cantidad =
                    Integer.parseInt(cantidadTexto);

            double costoUnitario =
                    Double.parseDouble(costoTexto);

            if (cantidad <= 0) {

                mostrarError(
                        "La cantidad debe ser mayor que 0."
                );

                return;
            }

            if (costoUnitario < 0) {

                mostrarError(
                        "El costo unitario no puede ser negativo."
                );

                return;
            }

            DetalleCompra detalle =
                    new DetalleCompra(
                            idProducto,
                            cantidad,
                            costoUnitario
                    );

            listaDetalles.add(detalle);

            /*
             * Se limpian solamente los campos del detalle,
             * permitiendo ingresar otro producto a la misma orden.
             */
            txtIdProducto.clear();
            txtCantidad.clear();
            txtCostoUnitario.clear();

        } catch (NumberFormatException e) {

            mostrarError(
                    "La cantidad debe ser un entero y el costo debe ser numérico."
            );
        }
    }

    /**
     * Registra una nueva orden de compra.
     *
     * La orden se crea inicialmente en estado Pendiente.
     * Las validaciones y reglas de negocio son realizadas
     * por el {@link CompraService}.
     */
    @FXML
    public void guardarCompra() {

        String idOrden =
                txtIdOrden.getText().trim();

        String idProveedor =
                txtIdProveedor.getText().trim();

        String fecha =
                txtFecha.getText().trim();

        if (idOrden.isEmpty()
                || idProveedor.isEmpty()
                || fecha.isEmpty()) {

            mostrarError(
                    "Debe completar todos los datos de la orden."
            );

            return;
        }

        if (listaDetalles.isEmpty()) {

            mostrarError(
                    "La orden debe tener al menos un producto."
            );

            return;
        }

        /*
         * Se crea una copia de la lista temporal para que la
         * orden tenga sus propios detalles.
         */
        List<DetalleCompra> detalles =
                new ArrayList<>(listaDetalles);

        /*
         * El estado inicial de una orden nueva es Pendiente.
         */
        OrdenCompra nueva =
                new OrdenCompra(
                        idOrden,
                        idProveedor,
                        fecha,
                        "Pendiente",
                        detalles
                );

        try {

            servicio.registrarCompra(nueva);

            cargarDatos();
            limpiarCampos();

        } catch (IllegalArgumentException e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Marca como recibida la orden seleccionada.
     *
     * El servicio se encarga de cambiar el estado de la orden
     * y registrar las entradas correspondientes en el inventario.
     */
    @FXML
    public void recibirCompra() {

        OrdenCompra seleccionada =
                tablaCompras.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {

            mostrarError(
                    "Debe seleccionar una orden de compra."
            );

            return;
        }

        String idOrden = seleccionada.getIdOrden();

        // Recibir una orden cambia su estado y registra entradas de inventario,
        // así que corre en un hilo aparte; al terminar se recarga la tabla.
        ejecutorRecepcion.ejecutar(
                () -> {
                    servicio.recibirCompra(idOrden);
                    return null;
                },
                resultado -> cargarDatos(),
                this::mostrarError
        );
    }

    /**
     * Elimina de la orden el producto seleccionado en la tabla de detalles.
     *
     * El producto solamente se elimina de la lista temporal de la orden
     * y no afecta la base de datos ni el inventario, ya que la orden
     * todavía no ha sido registrada.
     */
    @FXML
    public void eliminarDetalle() {

        DetalleCompra seleccionado =
                tablaDetalles.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarError(
                    "Debe seleccionar un producto de la tabla de detalles."
            );
            return;
        }

        listaDetalles.remove(seleccionado);
    }
    /**
     * Cancela la orden de compra seleccionada.
     *
     * Una orden cancelada no modifica el inventario.
     * El servicio se encarga de validar que la transición
     * de estado sea permitida.
     */
    @FXML
    public void cancelarCompra() {

        OrdenCompra seleccionada =
                tablaCompras.getSelectionModel().getSelectedItem();

        if (seleccionada == null) {

            mostrarError(
                    "Debe seleccionar una orden de compra."
            );

            return;
        }

        try {

            servicio.cancelarCompra(
                    seleccionada.getIdOrden()
            );

            cargarDatos();

        } catch (IllegalArgumentException e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Limpia los campos utilizados para registrar una nueva orden.
     *
     * También elimina los detalles que todavía no hayan sido
     * registrados dentro de una orden.
     */
    private void limpiarCampos() {

        txtIdOrden.clear();
        txtIdProveedor.clear();
        txtFecha.clear();

        txtIdProducto.clear();
        txtCantidad.clear();
        txtCostoUnitario.clear();

        listaDetalles.clear();
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
}