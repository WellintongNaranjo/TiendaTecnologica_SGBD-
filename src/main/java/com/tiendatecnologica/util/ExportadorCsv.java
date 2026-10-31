package com.tiendatecnologica.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Escribe tablas de datos en archivos CSV (UTF-8) pensados para abrirse en Excel.
 * Los campos se separan con punto y coma, porque Excel con configuración regional
 * en español usa ese separador y con comas dejaría todo en una sola columna.
 * Los campos con separador, comillas dobles o saltos de línea van entre comillas,
 * y las comillas dobles internas se duplican.
 *
 * El contenido se arma completo en memoria y se escribe primero en un archivo
 * temporal que reemplaza al destino solo cuando la escritura terminó bien; así,
 * si algo falla no queda un archivo vacío o a medias.
 *
 * Se escribe un BOM UTF-8 al inicio para que Excel muestre bien las tildes, y una
 * primera línea {@code sep=;} que le indica a Excel el separador sin importar su
 * configuración regional (esa línea no se muestra como fila en Excel).
 * Esta clase no depende de JavaFX ni de la base de datos.
 */
public final class ExportadorCsv {

    /** Separador de filas que pide RFC 4180. */
    private static final String SALTO_LINEA = "\r\n";

    /** Marca de orden de bytes UTF-8 (hace que Excel detecte la codificación). */
    private static final String BOM = "\uFEFF";

    /** Separador de columnas. */
    private static final char SEPARADOR = ';';

    private ExportadorCsv() {
        // Clase utilitaria, no se instancia
    }

    /**
     * Escribe un CSV con encabezados y filas en el archivo indicado.
     *
     * @param destino     ruta del archivo a crear o reemplazar
     * @param encabezados nombres de las columnas
     * @param filas       datos; cada fila debe tener tantos valores como encabezados (null se escribe vacío)
     * @throws IOException              si no se puede escribir el archivo
     * @throws IllegalArgumentException si alguna fila no tiene el mismo número de columnas que el encabezado
     */
    public static void escribir(Path destino, String[] encabezados, List<String[]> filas) throws IOException {
        String contenido = construir(encabezados, filas);

        Path absoluto = destino.toAbsolutePath();
        Path temporal = Files.createTempFile(absoluto.getParent(), "reporte-", ".tmp");
        try {
            Files.write(temporal, contenido.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(temporal, absoluto,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal, absoluto, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporal);   // si el move funcionó, ya no existe
        }
    }

    /**
     * Arma el texto completo del CSV (BOM, línea sep=;, encabezado y filas).
     *
     * @param encabezados nombres de las columnas
     * @param filas       datos de las filas
     * @return el contenido del archivo como texto
     * @throws IllegalArgumentException si alguna fila tiene un número distinto de columnas
     */
    static String construir(String[] encabezados, List<String[]> filas) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("sep=").append(SEPARADOR).append(SALTO_LINEA);
        agregarFila(sb, encabezados);
        for (String[] fila : filas) {
            if (fila.length != encabezados.length) {
                throw new IllegalArgumentException("Una fila tiene " + fila.length
                        + " columnas pero el encabezado tiene " + encabezados.length);
            }
            agregarFila(sb, fila);
        }
        return sb.toString();
    }

    /**
     * Escapa un campo según las reglas de CSV.
     *
     * @param campo texto del campo (null se trata como vacío)
     * @return el campo listo para escribirse en el CSV
     */
    static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        boolean necesitaComillas = campo.indexOf(SEPARADOR) >= 0 || campo.indexOf('"') >= 0
                || campo.indexOf('\n') >= 0 || campo.indexOf('\r') >= 0;
        if (!necesitaComillas) {
            return campo;
        }
        return "\"" + campo.replace("\"", "\"\"") + "\"";
    }

    private static void agregarFila(StringBuilder sb, String[] valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                sb.append(SEPARADOR);
            }
            sb.append(escapar(valores[i]));
        }
        sb.append(SALTO_LINEA);
    }
}