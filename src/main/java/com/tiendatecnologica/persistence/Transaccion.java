package com.tiendatecnologica.persistence;

import java.sql.Connection;
import java.sql.SQLException;

public final class Transaccion {

    public interface Trabajo<T> {
        T ejecutar(Connection con) throws SQLException;
    }

    private Transaccion() {
    }

    public static <T> T ejecutar(Trabajo<T> trabajo) {
        try (Connection con = ConexionBD.getConexion()) {
            con.setAutoCommit(false);
            try {
                T resultado = trabajo.ejecutar(con);
                con.commit();
                return resultado;
            } catch (SQLException e) {
                revertir(con);
                throw new IllegalStateException(
                        "Error de base de datos: " + e.getMessage(), e);
            } catch (RuntimeException e) {
                revertir(con);
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    private static void revertir(Connection con) {
        try {
            con.rollback();
        } catch (SQLException e) {
            System.err.println("No se pudo hacer rollback: " + e.getMessage());
        }
    }
}
