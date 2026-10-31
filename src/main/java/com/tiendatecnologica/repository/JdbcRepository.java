package com.tiendatecnologica.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Base genérica para repositorios que guardan sus entidades en MySQL con JDBC.
 * Guarda y lee las entidades directamente en MySQL.
 *
 * <p>Usa el mismo patrón "método plantilla": cada repositorio concreto solo
 * dice cómo se llama su tabla, cuál es su clave, qué SQL usa para insertar y
 * actualizar y cómo convertir una fila ({@link ResultSet}) en su entidad.
 * Esta clase se encarga de abrir/cerrar la conexión, ejecutar con
 * PreparedStatement (nunca se concatenan valores en el SQL, así no hay
 * inyección SQL) y traducir cualquier SQLException a una
 * RepositorioException con un mensaje para el usuario.</p>
 *
 * <p><b>Plantilla para los demás repositorios:</b> copiar
 * {@link ProductoRepository}, cambiar tabla, columnas y mapeo.</p>
 *
 * @param <T> tipo de la entidad que administra el repositorio
 */

//crud generico Solo ejecuta el SQL que le den y ya
//como es la base de los demas repositoys llama a jdbs support para poder hacer las funciones sql sin repetir codigo
public abstract class JdbcRepository<T> extends JdbcSupport implements Repository<T> {

    // ------------------------------------------------------------------
    // Lo que debe definir cada repositorio concreto
    // ------------------------------------------------------------------

    /** @return nombre de la tabla en MySQL */
    protected abstract String tabla();

    /** @return nombre de la columna que es la clave primaria (de tipo texto) */
    protected abstract String columnaClave();

    /** @return nombre de la entidad para los mensajes de error (ej. "producto") */
    protected abstract String entidad();

    /** @return INSERT con un '?' por cada valor de {@link #parametrosInsertar} */
    protected abstract String sqlInsertar();

    /** @return UPDATE que termina en {@code WHERE <clave> = ?} */
    protected abstract String sqlActualizar();

    /** @return valores del INSERT, en el mismo orden que los '?' */
    protected abstract Object[] parametrosInsertar(T entidad);

    /** @return valores del UPDATE; el último debe ser la clave (del WHERE) */
    protected abstract Object[] parametrosActualizar(T entidad);

    /** Construye la entidad a partir de la fila actual del ResultSet. */
    protected abstract T mapear(ResultSet rs) throws SQLException;

    // ------------------------------------------------------------------
    // CRUD (implementación común)
    // ------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public List<T> obtenerTodos() {
        return consultar("SELECT * FROM " + tabla() + " ORDER BY " + columnaClave(),
                "consultar los registros de " + entidad(),
                this::mapear);
    }

    /** {@inheritDoc} */
    @Override
    public T buscarPorClave(String clave) {
        List<T> resultado = consultar(
                "SELECT * FROM " + tabla() + " WHERE " + columnaClave() + " = ?",
                "buscar el " + entidad(),
                this::mapear, clave);
        return resultado.isEmpty() ? null : resultado.get(0);
    }

    /** {@inheritDoc} */
    @Override
    public void guardar(T entidad) {
        ejecutar(sqlInsertar(), "guardar el " + entidad(), parametrosInsertar(entidad));
    }

    /** {@inheritDoc} */
    @Override
    public void actualizar(T entidad) {
        ejecutar(sqlActualizar(), "actualizar el " + entidad(), parametrosActualizar(entidad));
    }

    /** {@inheritDoc} */
    @Override
    public void eliminar(String clave) {
        ejecutar("DELETE FROM " + tabla() + " WHERE " + columnaClave() + " = ?",
                "eliminar el " + entidad(), clave);
    }

    /** {@inheritDoc} */
    @Override
    public boolean existe(String clave) {
        List<Integer> resultado = consultar(
                "SELECT 1 FROM " + tabla() + " WHERE " + columnaClave() + " = ?",
                "verificar si existe el " + entidad(),
                rs -> 1, clave);
        return !resultado.isEmpty();
    }

    /**
     * Calcula el siguiente id con formato {@code PREFIJO-00001} mirando el
     * mayor número que ya existe en la tabla. Reemplaza a
     * {@code GeneradorId} para los repositorios que heredan de esta clase.
     *
     * @param prefijo prefijo del id incluyendo el guion (ej. "PROV-")
     * @return el id siguiente, listo para usar
     */
    public String siguienteId(String prefijo) {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(" + columnaClave() + ", ?) AS UNSIGNED)), 0) "
                + "FROM " + tabla() + " WHERE " + columnaClave() + " LIKE ?";
        List<Integer> resultado = consultar(sql, "generar el id de " + entidad(),
                rs -> rs.getInt(1), prefijo.length() + 1, prefijo + "%");
        int ultimo = resultado.isEmpty() ? 0 : resultado.get(0);
        return prefijo + String.format("%05d", ultimo + 1);
    }
}
