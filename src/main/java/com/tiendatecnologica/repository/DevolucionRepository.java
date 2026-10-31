package com.tiendatecnologica.repository;

import com.tiendatecnologica.enums.EstadoDevolucion;
import com.tiendatecnologica.model.Devolucion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio de solicitudes de devolución, guardadas en la tabla
 * {@code devolucion} de MySQL. Hereda el CRUD genérico de {@link JdbcRepository}
 * y solo define el SQL y el mapeo de la entidad {@link Devolucion}.
 *
 * La tabla tiene claves foráneas hacia {@code venta} y {@code producto}, y un
 * CHECK que exige {@code cantidad > 0}; ambos errores llegan al usuario como
 * {@code RepositorioException} con un mensaje claro.
 */
public class DevolucionRepository extends JdbcRepository<Devolucion> {

    /** {@inheritDoc} */
    @Override
    protected String tabla() {
        return "devolucion";
    }

    /** {@inheritDoc} */
    @Override
    protected String columnaClave() {
        return "id";
    }

    /** {@inheritDoc} */
    @Override
    protected String entidad() {
        return "devolución";
    }

    /** {@inheritDoc} */
    @Override
    protected String sqlInsertar() {
        return "INSERT INTO devolucion "
                + "(id, id_venta, codigo_producto, cantidad, fecha_solicitud, motivo, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    /** {@inheritDoc} */
    @Override
    protected String sqlActualizar() {
        return "UPDATE devolucion SET id_venta = ?, codigo_producto = ?, cantidad = ?, "
                + "fecha_solicitud = ?, motivo = ?, estado = ? WHERE id = ?";
    }

    /** {@inheritDoc} */
    @Override
    protected Object[] parametrosInsertar(Devolucion d) {
        return new Object[] {
                d.getId(), d.getIdVenta(), d.getCodigoProducto(), d.getCantidad(),
                d.getFechaSolicitud(), d.getMotivo(), d.getEstado().name()
        };
    }

    /** {@inheritDoc} */
    @Override
    protected Object[] parametrosActualizar(Devolucion d) {
        return new Object[] {
                d.getIdVenta(), d.getCodigoProducto(), d.getCantidad(),
                d.getFechaSolicitud(), d.getMotivo(), d.getEstado().name(), d.getId()
        };
    }

    /** {@inheritDoc} */
    @Override
    protected Devolucion mapear(ResultSet rs) throws SQLException {
        return new Devolucion(
                rs.getString("id"),
                rs.getString("id_venta"),
                rs.getString("codigo_producto"),
                rs.getInt("cantidad"),
                rs.getObject("fecha_solicitud", LocalDateTime.class),
                rs.getString("motivo"),
                convertirEstado(rs.getString("estado"))
        );
    }

    /**
     * Obtiene las solicitudes de devolución asociadas a una venta específica.
     * El filtro se aplica en SQL (no se cargan todas las devoluciones).
     *
     * @param idVenta id de la venta a filtrar
     * @return lista de devoluciones de esa venta, ordenadas por id
     */
    public List<Devolucion> obtenerPorVenta(String idVenta) {
        return consultar("SELECT * FROM devolucion WHERE id_venta = ? ORDER BY id",
                "consultar las devoluciones de la venta", this::mapear, idVenta);
    }

    /**
     * Obtiene las devoluciones que están en un estado dado. El filtro se aplica en SQL.
     *
     * @param estado estado a filtrar; si es null se devuelven todas las devoluciones
     * @return lista de devoluciones en ese estado, ordenadas por id
     */
    public List<Devolucion> obtenerPorEstado(EstadoDevolucion estado) {
        if (estado == null) {
            return obtenerTodos();
        }
        return consultar("SELECT * FROM devolucion WHERE estado = ? ORDER BY id",
                "consultar las devoluciones por estado", this::mapear, estado.name());
    }

    /**
     * Cambia el estado de una devolución usando una conexión que ya está abierta,
     * para poder hacerlo dentro de una {@code Transaccion} junto con otras
     * operaciones (por ejemplo, el reingreso al inventario al aprobarla).
     * No cierra la conexión ni confirma la transacción: eso lo hace quien la abrió.
     *
     * @param con      conexión de la transacción en curso
     * @param id       id de la devolución
     * @param estado   nuevo estado
     * @throws SQLException si MySQL rechaza la actualización
     */
    public void actualizarEstado(Connection con, String id, EstadoDevolucion estado) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE devolucion SET estado = ? WHERE id = ?")) {
            ps.setString(1, estado.name());
            ps.setString(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Convierte el texto del ENUM de MySQL al enum de Java.
     *
     * @param texto valor de la columna {@code estado}
     * @return el {@link EstadoDevolucion} correspondiente
     * @throws SQLException si el valor no corresponde a ningún estado conocido
     */
    private static EstadoDevolucion convertirEstado(String texto) throws SQLException {
        try {
            return EstadoDevolucion.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SQLException("Estado de devolución desconocido en la base de datos: " + texto, e);
        }
    }
}
