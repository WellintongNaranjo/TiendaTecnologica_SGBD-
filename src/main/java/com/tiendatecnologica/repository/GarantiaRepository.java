package com.tiendatecnologica.repository;

import com.tiendatecnologica.enums.EstadoGarantia;
import com.tiendatecnologica.model.Garantia;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de solicitudes de garantía, guardadas en la tabla {@code garantia}
 * de MySQL. Hereda el CRUD genérico de {@link JdbcRepository} y solo define el
 * SQL y el mapeo de la entidad {@link Garantia}.
 *
 * La tabla tiene claves foráneas hacia {@code venta} y {@code producto}: si la
 * venta o el producto no existen en MySQL, el INSERT falla y se lanza una
 * {@code RepositorioException} con un mensaje para el usuario.
 */
public class GarantiaRepository extends JdbcRepository<Garantia> {

    /** {@inheritDoc} */
    @Override
    protected String tabla() {
        return "garantia";
    }

    /** {@inheritDoc} */
    @Override
    protected String columnaClave() {
        return "id";
    }

    /** {@inheritDoc} */
    @Override
    protected String entidad() {
        return "garantía";
    }

    /** {@inheritDoc} */
    @Override
    protected String sqlInsertar() {
        return "INSERT INTO garantia "
                + "(id, id_venta, codigo_producto, fecha_solicitud, descripcion_problema, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String sqlActualizar() {
        return "UPDATE garantia SET id_venta = ?, codigo_producto = ?, fecha_solicitud = ?, "
                + "descripcion_problema = ?, estado = ? WHERE id = ?";
    }

    /** {@inheritDoc} */
    @Override
    protected Object[] parametrosInsertar(Garantia g) {
        return new Object[] {
                g.getId(), g.getIdVenta(), g.getCodigoProducto(),
                g.getFechaSolicitud(), g.getDescripcionProblema(), g.getEstado().name()
        };
    }

    /** {@inheritDoc} */
    @Override
    protected Object[] parametrosActualizar(Garantia g) {
        return new Object[] {
                g.getIdVenta(), g.getCodigoProducto(), g.getFechaSolicitud(),
                g.getDescripcionProblema(), g.getEstado().name(), g.getId()
        };
    }

    /** {@inheritDoc} */
    @Override
    protected Garantia mapear(ResultSet rs) throws SQLException {
        return new Garantia(
                rs.getString("id"),
                rs.getString("id_venta"),
                rs.getString("codigo_producto"),
                rs.getObject("fecha_solicitud", LocalDateTime.class),
                rs.getString("descripcion_problema"),
                convertirEstado(rs.getString("estado"))
        );
    }

    /**
     * Obtiene las solicitudes de garantía asociadas a una venta específica.
     * El filtro se aplica en SQL (no se cargan todas las garantías).
     *
     * @param idVenta id de la venta a filtrar
     * @return lista de garantías de esa venta, ordenadas por id
     */
    public List<Garantia> obtenerPorVenta(String idVenta) {
        return consultar("SELECT * FROM garantia WHERE id_venta = ? ORDER BY id",
                "consultar las garantías de la venta", this::mapear, idVenta);
    }

    /**
     * Obtiene las garantías que están en un estado dado. El filtro se aplica en SQL.
     *
     * @param estado estado a filtrar; si es null se devuelven todas las garantías
     * @return lista de garantías en ese estado, ordenadas por id
     */
    public List<Garantia> obtenerPorEstado(EstadoGarantia estado) {
        if (estado == null) {
            return obtenerTodos();
        }
        return consultar("SELECT * FROM garantia WHERE estado = ? ORDER BY id",
                "consultar las garantías por estado", this::mapear, estado.name());
    }

    /**
     * Convierte el texto del ENUM de MySQL al enum de Java.
     *
     * @param texto valor de la columna {@code estado}
     * @return el {@link EstadoGarantia} correspondiente
     * @throws SQLException si el valor no corresponde a ningún estado conocido
     */
    private static EstadoGarantia convertirEstado(String texto) throws SQLException {
        try {
            return EstadoGarantia.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SQLException("Estado de garantía desconocido en la base de datos: " + texto, e);
        }
    }
}
