package com.tiendatecnologica.repository;

import com.tiendatecnologica.enums.EstadoVenta;
import com.tiendatecnologica.enums.MetodoPago;
import com.tiendatecnologica.model.Pago;
import com.tiendatecnologica.model.Venta;
import com.tiendatecnologica.persistence.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class VentaRepository implements Repository<Venta> {

    private static final String COLUMNAS =
            "id, fecha, id_cliente, subtotal, impuesto, total, "
                    + "metodo_pago, monto_recibido, vuelto, estado";

    @Override
    public List<Venta> obtenerTodos() {
        String sql = "SELECT " + COLUMNAS + " FROM venta ORDER BY fecha, id";
        List<Venta> ventas = new ArrayList<>();
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ventas.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw error("leer las ventas", e);
        }
        return ventas;
    }

    @Override
    public Venta buscarPorClave(String id) {
        try (Connection con = ConexionBD.getConexion()) {
            return buscarPorClave(con, id);
        } catch (SQLException e) {
            throw error("buscar la venta " + id, e);
        }
    }

    @Override
    public void guardar(Venta venta) {
        try (Connection con = ConexionBD.getConexion()) {
            guardar(con, venta);
        } catch (SQLException e) {
            throw error("guardar la venta " + venta.getId(), e);
        }
    }

    @Override
    public void actualizar(Venta venta) {
        try (Connection con = ConexionBD.getConexion()) {
            actualizar(con, venta);
        } catch (SQLException e) {
            throw error("actualizar la venta " + venta.getId(), e);
        }
    }

    @Override
    public void eliminar(String id) {
        String sql = "DELETE FROM venta WHERE id = ?";
        try (Connection con = ConexionBD.getConexion();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw error("eliminar la venta " + id, e);
        }
    }

    @Override
    public boolean existe(String id) {
        return buscarPorClave(id) != null;
    }

    public Venta buscarPorClave(Connection con, String id) throws SQLException {
        String sql = "SELECT " + COLUMNAS + " FROM venta WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void guardar(Connection con, Venta v) throws SQLException {
        String sql = "INSERT INTO venta (" + COLUMNAS + ") VALUES (?,?,?,?,?,?,?,?,?,?)";
        Pago pago = v.getPago();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getId());
            ps.setTimestamp(2, Timestamp.valueOf(v.getFecha()));
            ps.setString(3, v.getIdCliente());
            ps.setBigDecimal(4, java.math.BigDecimal.valueOf(v.getSubtotal()));
            ps.setBigDecimal(5, java.math.BigDecimal.valueOf(v.getImpuesto()));
            ps.setBigDecimal(6, java.math.BigDecimal.valueOf(v.getTotal()));
            ps.setString(7, pago.getMetodoPago().name());
            ps.setBigDecimal(8, java.math.BigDecimal.valueOf(pago.getMontoRecibido()));
            ps.setBigDecimal(9, java.math.BigDecimal.valueOf(pago.getVuelto()));
            ps.setString(10, v.getEstado().name());
            ps.executeUpdate();
        }
    }

    public void actualizar(Connection con, Venta v) throws SQLException {
        String sql = "UPDATE venta SET estado = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, v.getEstado().name());
            ps.setString(2, v.getId());
            ps.executeUpdate();
        }
    }

    private Venta mapear(ResultSet rs) throws SQLException {
        Pago pago = Pago.reconstruir(
                MetodoPago.valueOf(rs.getString("metodo_pago")),
                rs.getDouble("monto_recibido"),
                rs.getDouble("vuelto"));
        return new Venta(
                rs.getString("id"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                rs.getString("id_cliente"),
                rs.getDouble("subtotal"),
                rs.getDouble("impuesto"),
                rs.getDouble("total"),
                pago,
                EstadoVenta.valueOf(rs.getString("estado")));
    }

    private IllegalStateException error(String accion, SQLException e) {
        return new IllegalStateException(
                "Error de base de datos al " + accion + ": " + e.getMessage(), e);
    }
}
