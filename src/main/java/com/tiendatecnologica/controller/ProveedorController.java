package com.tiendatecnologica.controller;

import com.tiendatecnologica.model.Proveedor;
import com.tiendatecnologica.service.ProveedorService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

/**
 * Controlador de la pantalla de gestión de proveedores.
 *
 * Permite registrar, modificar, consultar y eliminar proveedores.
 *
 * La lógica de negocio se delega al {@link ProveedorService}.
 * El controlador se encarga de conectar la interfaz gráfica
 * con el servicio correspondiente.
 */
public class ProveedorController {

    /** Campo para ingresar el identificador del proveedor. */
    @FXML
    private TextField txtId;

    /** Campo para ingresar el nombre del proveedor. */
    @FXML
    private TextField txtNombre;

    /** Campo para ingresar la persona de contacto. */
    @FXML
    private TextField txtContacto;

    /** Campo para ingresar el número telefónico. */
    @FXML
    private TextField txtTelefono;

    /** Campo para ingresar el correo electrónico. */
    @FXML
    private TextField txtCorreo;

    /** Tabla que muestra los proveedores registrados. */
    @FXML
    private TableView<Proveedor> tablaProveedores;

    /** Columna que muestra el identificador del proveedor. */
    @FXML
    private TableColumn<Proveedor, String> colId;

    /** Columna que muestra el nombre del proveedor. */
    @FXML
    private TableColumn<Proveedor, String> colNombre;

    /** Columna que muestra la persona de contacto. */
    @FXML
    private TableColumn<Proveedor, String> colContacto;

    /** Columna que muestra el número telefónico. */
    @FXML
    private TableColumn<Proveedor, String> colTelefono;

    /** Columna que muestra el correo electrónico. */
    @FXML
    private TableColumn<Proveedor, String> colCorreo;

    /** Servicio encargado de gestionar los proveedores. */
    private final ProveedorService proveedorService =
            new ProveedorService();

    /** Lista observable utilizada para mostrar los proveedores en la tabla. */
    private ObservableList<Proveedor> listaProveedores;

    /** Ejecuta la carga de la tabla en un hilo aparte para no congelar la ventana. */
    private final EjecutorEnSegundoPlano ejecutorCarga = new EjecutorEnSegundoPlano();

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de la tabla, carga los proveedores
     * registrados y establece el evento que permite seleccionar
     * un proveedor para cargar sus datos en el formulario.
     */
    @FXML
    public void initialize() {

        colId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        colContacto.setCellValueFactory(
                new PropertyValueFactory<>("contacto")
        );

        colTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono")
        );

        colCorreo.setCellValueFactory(
                new PropertyValueFactory<>("correo")
        );

        // CAMBIO SQL: esta carga inicial ahora consulta MySQL.
        // cargarDatos() tiene su propio try/catch, así que si la base está
        // apagada se muestra una alerta y la pantalla no se rompe.
        cargarDatos();

        /*
         * Cuando se selecciona un proveedor de la tabla,
         * sus datos se cargan automáticamente en el formulario.
         */
        tablaProveedores.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (observable, anterior, seleccionado) -> {

                            if (seleccionado != null) {
                                cargarProveedorSeleccionado(
                                        seleccionado
                                );
                            }
                        }
                );
    }

    /**
     * Carga todos los proveedores registrados desde el servicio
     * y los muestra en la tabla.
     */
    private void cargarDatos() {

        // La consulta corre en un hilo aparte. Si falla (por ejemplo, MySQL apagado),
        // se muestra una alerta con el mensaje de RepositorioException en lugar de
        // cerrar la aplicación.
        ejecutorCarga.ejecutar(
                proveedorService::obtenerProveedores,
                proveedores -> {
                    listaProveedores = FXCollections.observableArrayList(proveedores);
                    tablaProveedores.setItems(listaProveedores);
                },
                this::mostrarError
        );
    }

    /**
     * Registra un nuevo proveedor utilizando los datos
     * ingresados en el formulario.
     */
    @FXML
    public void guardarProveedor() {

        try {

            Proveedor nuevo =
                    obtenerProveedorDeCampos();

            proveedorService.registrarProveedor(nuevo);

            mostrarInformacion(
                    "Proveedor registrado correctamente."
            );

            cargarDatos();
            limpiarCampos();

        } catch (Exception e) {

            // Sin cambios: los errores de MySQL (RepositorioException) también
            // caen aquí y su mensaje ya viene listo para el usuario.
            mostrarError(e.getMessage());
        }
    }

    /**
     * Modifica el proveedor seleccionado en la tabla.
     *
     * El identificador del proveedor se conserva y solamente
     * se actualizan sus demás datos.
     */
    @FXML
    public void modificarProveedor() {

        try {

            Proveedor seleccionado =
                    tablaProveedores
                            .getSelectionModel()
                            .getSelectedItem();

            if (seleccionado == null) {

                throw new IllegalArgumentException(
                        "Debe seleccionar un proveedor de la tabla."
                );
            }

            Proveedor actualizado =
                    new Proveedor(
                            seleccionado.getId(),
                            txtNombre.getText(),
                            txtContacto.getText(),
                            txtTelefono.getText(),
                            txtCorreo.getText()
                    );

            proveedorService.actualizarProveedor(
                    actualizado
            );

            mostrarInformacion(
                    "Proveedor modificado correctamente."
            );

            cargarDatos();
            limpiarCampos();

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Elimina el proveedor seleccionado en la tabla.
     *
     * Antes de eliminarlo, solicita una confirmación al usuario.
     */
    @FXML
    public void eliminarProveedor() {

        try {

            Proveedor seleccionado =
                    tablaProveedores
                            .getSelectionModel()
                            .getSelectedItem();

            if (seleccionado == null) {

                throw new IllegalArgumentException(
                        "Debe seleccionar un proveedor de la tabla."
                );
            }

            Alert confirmacion =
                    new Alert(
                            Alert.AlertType.CONFIRMATION,
                            "¿Está seguro de eliminar al proveedor "
                                    + seleccionado.getNombre() + "?"
                    );

            confirmacion.setHeaderText(
                    "Eliminar proveedor"
            );

            Optional<ButtonType> resultado =
                    confirmacion.showAndWait();

            if (resultado.isPresent()
                    && resultado.get() == ButtonType.OK) {

                // CAMBIO SQL: si el proveedor tiene órdenes de compra, MySQL rechaza
                // el borrado (clave foránea) y el catch muestra el mensaje claro.
                // Sus asociaciones con productos se borran solas (ON DELETE CASCADE).
                proveedorService.eliminarProveedor(
                        seleccionado.getId()
                );

                mostrarInformacion(
                        "Proveedor eliminado correctamente."
                );

                cargarDatos();
                limpiarCampos();
            }

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Carga los datos del proveedor seleccionado en los
     * campos correspondientes del formulario.
     *
     * El identificador se bloquea porque no debe modificarse
     * durante una actualización.
     *
     * @param proveedor proveedor seleccionado en la tabla
     */
    private void cargarProveedorSeleccionado(
            Proveedor proveedor) {

        txtId.setText(proveedor.getId());
        txtNombre.setText(proveedor.getNombre());
        txtContacto.setText(proveedor.getContacto());
        txtTelefono.setText(proveedor.getTelefono());
        txtCorreo.setText(proveedor.getCorreo());

        txtId.setDisable(true);
    }

    /**
     * Crea un objeto Proveedor utilizando los datos
     * ingresados en el formulario.
     *
     * @return proveedor creado con los datos del formulario
     */
    private Proveedor obtenerProveedorDeCampos() {

        return new Proveedor(
                txtId.getText().trim(),
                txtNombre.getText().trim(),
                txtContacto.getText().trim(),
                txtTelefono.getText().trim(),
                txtCorreo.getText().trim()
        );
    }

    /**
     * Limpia todos los campos del formulario.
     *
     * También habilita nuevamente el campo del identificador
     * y elimina la selección actual de la tabla.
     */
    @FXML
    private void limpiarCampos() {

        txtId.clear();
        txtNombre.clear();
        txtContacto.clear();
        txtTelefono.clear();
        txtCorreo.clear();

        txtId.setDisable(false);

        tablaProveedores
                .getSelectionModel()
                .clearSelection();
    }

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param mensaje mensaje que se mostrará
     */
    private void mostrarError(String mensaje) {

        Alert alerta =
                new Alert(
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
    private void mostrarInformacion(String mensaje) {

        Alert alerta =
                new Alert(
                        Alert.AlertType.INFORMATION,
                        mensaje
                );

        alerta.setHeaderText(
                "Operación completada"
        );

        alerta.showAndWait();
    }
}