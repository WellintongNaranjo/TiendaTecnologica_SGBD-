package com.tiendatecnologica.controller;

import com.tiendatecnologica.model.Producto;
import com.tiendatecnologica.model.Proveedor;
import com.tiendatecnologica.service.ProductoProveedorService;
import com.tiendatecnologica.service.ProductoService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

/**
 * Controlador de la pantalla de gestión de productos.
 *
 * Permite registrar, modificar, eliminar y consultar productos.
 * También permite seleccionar un producto de la tabla para cargar
 * sus datos en el formulario y administrar sus proveedores.
 *
 * La lógica de negocio se delega a {@link ProductoService} y
 * {@link ProductoProveedorService}. El controlador se encarga de
 * conectar la interfaz gráfica con los servicios.
 */
public class ProductoController {

    /** Servicio encargado de gestionar los productos. */
    private final ProductoService productoService =
            new ProductoService();

    /** CAMBIO ASOCIACIÓN: servicio de la relación producto-proveedor. */
    private final ProductoProveedorService asociacionService =
            new ProductoProveedorService();

    /** Ejecuta la carga de la tabla en un hilo aparte para no congelar la ventana. */
    private final EjecutorEnSegundoPlano ejecutorCarga = new EjecutorEnSegundoPlano();

    /** Ejecuta las búsquedas en un hilo aparte (separado de la carga completa). */
    private final EjecutorEnSegundoPlano ejecutorBusqueda = new EjecutorEnSegundoPlano();

    /** Campo para ingresar el código del producto. */
    @FXML
    private TextField campoCodigo;

    /** Campo para ingresar el nombre del producto. */
    @FXML
    private TextField campoNombre;

    /** Campo para ingresar la categoría del producto. */
    @FXML
    private TextField campoCategoria;

    /** Campo para ingresar la marca del producto. */
    @FXML
    private TextField campoMarca;

    /** Campo para ingresar el precio de compra. */
    @FXML
    private TextField campoPrecioCompra;

    /** Campo para ingresar el precio de venta. */
    @FXML
    private TextField campoPrecioVenta;

    /** Campo para ingresar la cantidad disponible. */
    @FXML
    private TextField campoCantidad;

    /** Campo para ingresar el stock mínimo. */
    @FXML
    private TextField campoStockMinimo;

    /** Campo para ingresar la cantidad de meses de garantía. */
    @FXML
    private TextField campoMesesGarantia;

    /** Campo utilizado para buscar productos. */
    @FXML
    private TextField campoBusqueda;

    /** Tabla que muestra los productos registrados. */
    @FXML
    private TableView<Producto> tablaProductos;

    /** Columna que muestra el código del producto. */
    @FXML
    private TableColumn<Producto, String> colCodigo;

    /** Columna que muestra el nombre del producto. */
    @FXML
    private TableColumn<Producto, String> colNombre;

    /** Columna que muestra la categoría del producto. */
    @FXML
    private TableColumn<Producto, String> colCategoria;

    /** Columna que muestra la marca del producto. */
    @FXML
    private TableColumn<Producto, String> colMarca;

    /** Columna que muestra el precio de venta. */
    @FXML
    private TableColumn<Producto, Number> colPrecioVenta;

    /** Columna que muestra la cantidad disponible. */
    @FXML
    private TableColumn<Producto, Number> colCantidad;

    // ------------------------------------------------------------------
    // CAMBIO ASOCIACIÓN: elementos del panel de proveedores del producto
    // ------------------------------------------------------------------

    /** Tabla con los proveedores que surten el producto seleccionado. */
    @FXML
    private TableView<Proveedor> tablaProveedoresProducto;

    /** Columna del id del proveedor. */
    @FXML
    private TableColumn<Proveedor, String> colProvId;

    /** Columna del nombre del proveedor. */
    @FXML
    private TableColumn<Proveedor, String> colProvNombre;

    /** Columna del teléfono del proveedor. */
    @FXML
    private TableColumn<Proveedor, String> colProvTelefono;

    /** Selector con los proveedores que todavía no surten el producto. */
    @FXML
    private ComboBox<Proveedor> comboProveedores;

    /**
     * Inicializa el controlador.
     *
     * Configura las columnas de la tabla, establece el evento
     * de selección de productos y carga los productos registrados.
     */
    @FXML
    public void initialize() {

        colCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigo")
        );

        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("categoria")
        );

        colMarca.setCellValueFactory(
                new PropertyValueFactory<>("marca")
        );

        colPrecioVenta.setCellValueFactory(
                new PropertyValueFactory<>("precioVenta")
        );

        colCantidad.setCellValueFactory(
                new PropertyValueFactory<>("cantidadDisponible")
        );

        // CAMBIO ASOCIACIÓN: columnas de la tabla de proveedores del producto.
        colProvId.setCellValueFactory(
                new PropertyValueFactory<>("id")
        );

        colProvNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre")
        );

        colProvTelefono.setCellValueFactory(
                new PropertyValueFactory<>("telefono")
        );

        // CAMBIO ASOCIACIÓN: Proveedor no tiene toString(), así que el selector
        // necesita un convertidor que diga cómo mostrar cada proveedor ("id - nombre").
        comboProveedores.setConverter(new StringConverter<Proveedor>() {
            @Override
            public String toString(Proveedor proveedor) {
                return proveedor == null
                        ? ""
                        : proveedor.getId() + " - " + proveedor.getNombre();
            }

            @Override
            public Proveedor fromString(String texto) {
                return null; // el selector no es editable
            }
        });

        /*
         * Cuando el usuario selecciona un producto de la tabla,
         * sus datos se cargan automáticamente en el formulario.
         */
        tablaProductos.getSelectionModel()
                .selectedItemProperty()
                .addListener(
                        (obs, anterior, seleccionado) -> {

                            // El stock solo se escribe al crear un producto; con uno
                            // seleccionado el campo es de solo lectura porque el stock
                            // cambia únicamente por movimientos de inventario.
                            campoCantidad.setEditable(seleccionado == null);

                            if (seleccionado != null) {
                                cargarFormulario(seleccionado);

                                // CAMBIO ASOCIACIÓN: al elegir un producto se
                                // muestran sus proveedores en el panel inferior.
                                cargarPanelProveedores(seleccionado.getCodigo());

                            } else {

                                // CAMBIO ASOCIACIÓN: sin producto seleccionado,
                                // el panel de proveedores se vacía.
                                limpiarPanelProveedores();
                            }
                        }
                );

        // CAMBIO SQL: esta carga inicial ahora consulta MySQL.
        // cargarProductos() ya tiene su propio try/catch, así que si la base
        // está apagada se muestra una alerta y la pantalla no se rompe.
        cargarProductos();
    }

    /**
     * Registra un nuevo producto.
     *
     * Los datos se obtienen del formulario y las validaciones
     * correspondientes son realizadas por el servicio.
     */
    @FXML
    public void registrarProducto() {

        try {

            Producto producto =
                    leerFormulario();

            productoService.registrarProducto(producto);

            cargarProductos();
            limpiarFormulario();

        } catch (NumberFormatException e) {

            mostrarError("Precio, cantidad, stock mínimo y meses de garantía deben ser números válidos");

        } catch (Exception e) {

            // Sin cambios: RepositorioException (errores de MySQL) también cae aquí
            // y su mensaje ya viene listo para el usuario.
            mostrarError(e.getMessage());
        }
    }

    /**
     * Modifica los datos del producto seleccionado.
     *
     * Los datos actualizados se obtienen del formulario y se
     * envían al servicio para realizar las validaciones.
     */
    @FXML
    public void modificarProducto() {

        try {

            Producto producto =
                    leerFormulario();

            productoService.modificarProducto(producto);

            cargarProductos();
            limpiarFormulario();

        } catch (NumberFormatException e) {

            mostrarError("Precio, cantidad, stock mínimo y meses de garantía deben ser números válidos");

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Elimina el producto seleccionado de la tabla.
     */
    @FXML
    public void eliminarProducto() {

        Producto seleccionado =
                tablaProductos
                        .getSelectionModel()
                        .getSelectedItem();

        if (seleccionado == null) {

            mostrarError(
                    "Seleccione un producto de la tabla"
            );

            return;
        }

        try {

            // Sin cambios: sus asociaciones con proveedores se borran solas
            // (ON DELETE CASCADE). Si tiene ventas o compras, MySQL lo rechaza
            // y aquí se muestra el mensaje de RepositorioException.
            productoService.eliminarProducto(
                    seleccionado.getCodigo()
            );

            cargarProductos();
            limpiarFormulario();

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Busca productos utilizando el texto ingresado.
     *
     * El servicio realiza la búsqueda y la tabla se actualiza
     * con los resultados encontrados.
     */
    @FXML
    public void buscarProducto() {

        // La consulta corre en un hilo aparte; si falla (por ejemplo, MySQL apagado)
        // se muestra una alerta en lugar de cerrar la aplicación.
        String texto = campoBusqueda.getText();

        ejecutorBusqueda.ejecutar(
                () -> productoService.buscarProductos(texto),
                resultado -> tablaProductos.setItems(
                        FXCollections.observableArrayList(resultado)),
                this::mostrarError
        );
    }

    /**
     * Carga todos los productos registrados en la tabla.
     */
    @FXML
    public void cargarProductos() {

        // La consulta corre en un hilo aparte para que la ventana no se congele.
        // Si falla, se muestra una alerta en lugar de cerrar la aplicación.
        // Este método también se llama desde initialize(), así que cubre la carga inicial.
        ejecutorCarga.ejecutar(
                productoService::listarProductos,
                productos -> tablaProductos.setItems(
                        FXCollections.observableArrayList(productos)),
                this::mostrarError
        );
    }

    // ------------------------------------------------------------------
    // CAMBIO ASOCIACIÓN: acciones del panel de proveedores
    // ------------------------------------------------------------------

    /**
     * Asocia al producto seleccionado el proveedor elegido en el selector.
     */
    @FXML
    public void asociarProveedor() {

        Producto producto =
                tablaProductos.getSelectionModel().getSelectedItem();

        Proveedor proveedor =
                comboProveedores.getSelectionModel().getSelectedItem();

        if (producto == null) {

            mostrarError("Seleccione un producto de la tabla");

            return;
        }

        if (proveedor == null) {

            mostrarError("Seleccione un proveedor de la lista");

            return;
        }

        try {

            asociacionService.asociar(
                    producto.getCodigo(),
                    proveedor.getId()
            );

            // Se recargan la tabla y el selector para reflejar el cambio.
            cargarPanelProveedores(producto.getCodigo());

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Quita la asociación entre el producto seleccionado y el proveedor
     * seleccionado en la tabla del panel. El proveedor no se elimina,
     * solo deja de estar vinculado a ese producto.
     */
    @FXML
    public void quitarProveedor() {

        Producto producto =
                tablaProductos.getSelectionModel().getSelectedItem();

        Proveedor proveedor =
                tablaProveedoresProducto.getSelectionModel().getSelectedItem();

        if (producto == null) {

            mostrarError("Seleccione un producto de la tabla");

            return;
        }

        if (proveedor == null) {

            mostrarError("Seleccione el proveedor que desea quitar de la tabla inferior");

            return;
        }

        try {

            asociacionService.desasociar(
                    producto.getCodigo(),
                    proveedor.getId()
            );

            cargarPanelProveedores(producto.getCodigo());

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Llena el panel inferior: los proveedores que ya surten el producto
     * (tabla) y los que todavía no (selector).
     *
     * @param codigoProducto código del producto seleccionado
     */
    private void cargarPanelProveedores(String codigoProducto) {

        // Consulta MySQL (JOIN), así que lleva su propio try/catch.
        try {

            tablaProveedoresProducto.setItems(
                    FXCollections.observableArrayList(
                            asociacionService.listarProveedoresDeProducto(codigoProducto)
                    )
            );

            comboProveedores.setItems(
                    FXCollections.observableArrayList(
                            asociacionService.listarProveedoresDisponibles(codigoProducto)
                    )
            );

            comboProveedores.getSelectionModel().clearSelection();

        } catch (Exception e) {

            mostrarError(e.getMessage());
        }
    }

    /**
     * Vacía el panel de proveedores (cuando no hay producto seleccionado).
     * No consulta la base de datos, solo limpia la pantalla.
     */
    private void limpiarPanelProveedores() {

        tablaProveedoresProducto.getItems().clear();
        comboProveedores.getItems().clear();
    }

    /**
     * Limpia todos los campos del formulario y elimina
     * la selección actual de la tabla.
     */
    @FXML
    public void limpiarFormulario() {

        campoCodigo.clear();
        campoNombre.clear();
        campoCategoria.clear();
        campoMarca.clear();
        campoPrecioCompra.clear();
        campoPrecioVenta.clear();
        campoCantidad.clear();
        campoStockMinimo.clear();
        campoMesesGarantia.clear();

        // Al quitar la selección, el listener vacía también el panel de proveedores.
        tablaProductos
                .getSelectionModel()
                .clearSelection();
    }

    /**
     * Carga los datos del producto seleccionado en el formulario.
     *
     * @param producto producto seleccionado en la tabla
     */
    private void cargarFormulario(Producto producto) {

        campoCodigo.setText(
                producto.getCodigo()
        );

        campoNombre.setText(
                producto.getNombre()
        );

        campoCategoria.setText(
                producto.getCategoria()
        );

        campoMarca.setText(
                producto.getMarca()
        );

        campoPrecioCompra.setText(
                String.valueOf(producto.getPrecioCompra())
        );

        campoPrecioVenta.setText(
                String.valueOf(producto.getPrecioVenta())
        );

        campoCantidad.setText(
                String.valueOf(producto.getCantidadDisponible())
        );

        campoStockMinimo.setText(
                String.valueOf(producto.getStockMinimo())
        );

        campoMesesGarantia.setText(
                String.valueOf(producto.getMesesGarantia())
        );
    }

    /**
     * Crea un objeto Producto utilizando los datos
     * ingresados en el formulario.
     *
     * La conversión de los campos numéricos se realiza
     * antes de enviar el producto al servicio.
     *
     * @return producto creado con los datos del formulario
     */
    private Producto leerFormulario() {

        return new Producto(
                campoCodigo.getText(),
                campoNombre.getText(),
                campoCategoria.getText(),
                campoMarca.getText(),
                Double.parseDouble(
                        campoPrecioCompra.getText()
                ),
                Double.parseDouble(
                        campoPrecioVenta.getText()
                ),
                Integer.parseInt(
                        campoCantidad.getText()
                ),
                Integer.parseInt(
                        campoStockMinimo.getText()
                ),
                Integer.parseInt(
                        campoMesesGarantia.getText()
                )
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
}