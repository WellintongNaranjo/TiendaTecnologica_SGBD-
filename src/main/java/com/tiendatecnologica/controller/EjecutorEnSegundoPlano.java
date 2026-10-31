package com.tiendatecnologica.controller;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Ejecuta una operación de base de datos fuera del hilo de JavaFX con un
 * {@link Task}, para que la ventana no se congele. Lo comparten las pantallas
 * de garantías y devoluciones.
 *
 * Mientras una operación sigue en curso se ignoran las nuevas solicitudes de
 * la misma pantalla; así un doble clic no registra dos veces la misma solicitud.
 * Los resultados y los errores se entregan en el hilo de JavaFX (los manejadores
 * {@code setOnSucceeded} y {@code setOnFailed} de un Task ya corren allí), por lo
 * que {@code alTerminar} y {@code alFallar} pueden modificar controles directamente.
 *
 * Debe usarse siempre desde el hilo de JavaFX.
 */
public final class EjecutorEnSegundoPlano {

    /** Indica si hay una operación en curso; solo se toca desde el hilo de JavaFX. */
    private boolean enCurso;

    /**
     * Lanza la operación en un hilo aparte.
     *
     * El trabajo no debe tocar controles de JavaFX: los valores de la pantalla
     * se leen antes y se pasan al trabajo.
     *
     * @param trabajo   operación a ejecutar (por ejemplo, llamadas al servicio)
     * @param alTerminar qué hacer con el resultado cuando termina bien (hilo de JavaFX)
     * @param alFallar  qué hacer con el mensaje de error si falla (hilo de JavaFX)
     * @param <T>       tipo del resultado (puede ser Void devolviendo null)
     * @return true si la operación se lanzó; false si se ignoró porque ya había otra en curso
     */
    public <T> boolean ejecutar(Callable<T> trabajo, Consumer<T> alTerminar, Consumer<String> alFallar) {

        if (enCurso) {
            return false;
        }
        enCurso = true;

        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };

        tarea.setOnSucceeded(evento -> {
            enCurso = false;
            alTerminar.accept(tarea.getValue());
        });

        tarea.setOnFailed(evento -> {
            enCurso = false;
            Throwable error = tarea.getException();
            alFallar.accept(error == null || error.getMessage() == null
                    ? "Ocurrió un error inesperado"
                    : error.getMessage());
        });

        Thread hilo = new Thread(tarea, "operacion-bd");
        hilo.setDaemon(true);
        hilo.start();
        return true;
    }
}
