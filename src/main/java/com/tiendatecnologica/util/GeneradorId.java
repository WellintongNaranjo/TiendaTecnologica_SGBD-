package com.tiendatecnologica.util;

import com.tiendatecnologica.persistence.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class GeneradorId {

    private GeneradorId() {
    }

    public static String generarIdVenta(Connection con) {
        return generar(con, "V-", "venta", "id");
    }

    public static String generarIdCompra(Connection con) {
        return generar(con, "C-", "orden_compra", "id_orden");
    }

    public static String generarIdMovimiento(Connection con) {
        return generar(con, "MOV-", "movimiento_inventario", "id");
    }


    public static String generarIdVenta() {
        return generar("V-", "venta", "id");
    }

    public static String generarIdCompra() {
        return generar("C-", "orden_compra", "id_orden");
    }

    public static String generarIdMovimiento() {
        return generar("MOV-", "movimiento_inventario", "id");
    }

    public static String generarIdGarantia() {
        return generar("GAR-", "garantia", "id");
    }

    public static String generarIdDevolucion() {
        return generar("DEV-", "devolucion", "id");
    }

    public static String generarIdCliente() {
        return generar("CLI-", "cliente", "id");
    }

    private static String generar(String prefijo, String tabla, String columna) {
        try (Connection con = ConexionBD.getConexion()) {
            return generar(con, prefijo, tabla, columna);
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo generar el id de " + tabla + ": " + e.getMessage(), e);
        }
    }

    private static String generar(Connection con, String prefijo, String tabla, String columna) {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(" + columna + ", ?) AS UNSIGNED)), 0) + 1 "
                + "FROM " + tabla + " WHERE " + columna + " LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, prefijo.length() + 1);
            ps.setString(2, prefijo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return prefijo + String.format("%05d", rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo generar el id de " + tabla + ": " + e.getMessage(), e);
        }
    }
}
