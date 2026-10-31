package com.tiendatecnologica.controller;

import javafx.scene.control.ComboBox;

/**
 * Utilidad compartida por las pantallas de garantías y devoluciones para el
 * filtro por estado. Llena el ComboBox con "Todos" más los valores del enum y
 * traduce la selección de vuelta al enum.
 *
 * "Todos" es solo una opción de la interfaz: nunca se guarda en la base de datos.
 */
public final class FiltroEstado {

    /** Texto de la opción que muestra todos los registros. */
    public static final String TODOS = "Todos";

    private FiltroEstado() {
        // Clase utilitaria, no se instancia
    }

    /**
     * Llena el ComboBox con "Todos" y los estados del enum, deja "Todos"
     * seleccionado y avisa cuando el usuario cambia la selección.
     *
     * @param combo      ComboBox del FXML
     * @param tipoEstado enum de estados (ej. EstadoGarantia.class)
     * @param alCambiar  acción a ejecutar cuando el usuario elige otro estado
     * @param <E>        tipo del enum
     */
    public static <E extends Enum<E>> void configurar(ComboBox<String> combo, Class<E> tipoEstado,
                                                      Runnable alCambiar) {
        combo.getItems().add(TODOS);
        for (E estado : tipoEstado.getEnumConstants()) {
            combo.getItems().add(estado.name());
        }
        combo.setValue(TODOS);
        // El listener se agrega después de fijar el valor inicial para no recargar dos veces.
        combo.valueProperty().addListener((obs, anterior, nuevo) -> alCambiar.run());
    }

    /**
     * Devuelve el estado seleccionado en el ComboBox.
     *
     * @param combo      ComboBox configurado con {@link #configurar}
     * @param tipoEstado enum de estados
     * @param <E>        tipo del enum
     * @return el estado elegido, o null si está seleccionado "Todos" (sin filtro)
     */
    public static <E extends Enum<E>> E obtener(ComboBox<String> combo, Class<E> tipoEstado) {
        String valor = combo.getValue();
        if (valor == null || TODOS.equals(valor)) {
            return null;
        }
        return Enum.valueOf(tipoEstado, valor);
    }
}
