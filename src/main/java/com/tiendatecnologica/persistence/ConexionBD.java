package com.tiendatecnologica.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Clase encargada de abrir conexiones con la base de datos MySQL.
 *
 * Los datos de conexión (URL, usuario y contraseña) se leen del archivo
 * {@code db.properties}, ubicado en {@code src/main/resources}. Ese archivo
 * no se sube a Git; cada integrante copia {@code db.properties.example},
 * lo renombra a {@code db.properties} y completa sus propios datos.
 *
 * Cada llamada a {@link #getConexion()} devuelve una conexión NUEVA.
 * Quien la pida debe cerrarla (lo más simple es usar try-with-resources),
 * así varios hilos (por ejemplo los Task de JavaFX) no comparten una
 * misma conexión.
 *
 * La configuración se lee la primera vez que se necesita (no al cargar la
 * clase). Si falta el archivo o la BD no responde, se lanza una
 * {@link SQLException} normal que los repositorios convierten en un mensaje
 * para el usuario, en vez de cerrar la aplicación con un error de
 * inicialización.
 */
public final class ConexionBD {

    /** Nombre del archivo de configuración dentro de resources. */
    private static final String ARCHIVO_CONFIG = "db.properties";

    /** Milisegundos máximos de espera al conectar, para que la UI no se congele si MySQL está apagado. */
    private static final String TIMEOUT_CONEXION_MS = "5000";

    /** Datos de conexión, leídos una sola vez la primera vez que se piden. */
    private static Properties config;

    private ConexionBD() {
        // Clase utilitaria, no se instancia
    }

    /**
     * Abre una nueva conexión con la base de datos.
     *
     * Debe cerrarse después de usarla:
     * <pre>
     * try (Connection con = ConexionBD.getConexion()) {
     *     // usar la conexión
     * }
     * </pre>
     *
     * @return una conexión abierta a MySQL
     * @throws SQLException si no se puede conectar (servidor apagado,
     *                      usuario o contraseña incorrectos, falta
     *                      db.properties, etc.)
     */
    public static Connection getConexion() throws SQLException {
        Properties datos = obtenerConfiguracion();

        Properties info = new Properties();
        info.setProperty("user", datos.getProperty("db.user", ""));
        info.setProperty("password", datos.getProperty("db.password", ""));
        info.setProperty("connectTimeout", TIMEOUT_CONEXION_MS);

        return DriverManager.getConnection(datos.getProperty("db.url"), info);
    }

    /** Devuelve la configuración, leyéndola del classpath la primera vez. */
    private static synchronized Properties obtenerConfiguracion() throws SQLException {
        if (config == null) {
            config = cargarConfiguracion();
        }
        return config;
    }

    /**
     * Lee el archivo db.properties desde el classpath.
     *
     * @return las propiedades con los datos de conexión
     * @throws SQLException si el archivo no existe o no se puede leer
     */
    private static Properties cargarConfiguracion() throws SQLException {
        Properties props = new Properties();

        try (InputStream entrada = ConexionBD.class
                .getClassLoader().getResourceAsStream(ARCHIVO_CONFIG)) {

            if (entrada == null) {
                throw new SQLException(
                        "No se encontró " + ARCHIVO_CONFIG + " en src/main/resources. "
                                + "Copie db.properties.example como db.properties "
                                + "y complete sus datos.", "08001");
            }
            props.load(entrada);

        } catch (IOException e) {
            throw new SQLException("Error leyendo " + ARCHIVO_CONFIG, "08001", e);
        }
        return props;
    }
}
