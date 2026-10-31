package com.tiendatecnologica.repository;

import com.tiendatecnologica.model.Cliente;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ClienteRepository extends JdbcRepository<Cliente> {

    // Nombre de la tabla en MySQL.
    @Override
    protected String tabla() {
        return "cliente";
    }

    // Columna que identifica de forma única a cada cliente.
    @Override
    protected String columnaClave() {
        return "id";
    }

    // Palabra para los mensajes de error: "No se pudo guardar el cliente".
    @Override
    protected String entidad() {
        return "cliente";
    }

    // Las 5 columnas de la tabla, en orden, y 5 signos ? (uno por columna).
    @Override
    protected String sqlInsertar() {
        return "INSERT INTO cliente (id, cedula, nombre, telefono, email) "
                + "VALUES (?, ?, ?, ?, ?)";
    }

    // El UPDATE no modifica id: la clave solo se usa en el WHERE
    // para saber qué fila cambiar.
    @Override
    protected String sqlActualizar() {
        return "UPDATE cliente SET cedula = ?, nombre = ?, telefono = ?, "
                + "email = ? "
                + "WHERE id = ?";
    }

    // Valores del INSERT, en el mismo orden que las 5 columnas.
    // Se usa Object[] porque es el tipo que recibe JdbcSupport para los ?.
    @Override
    protected Object[] parametrosInsertar(Cliente c) {
        return new Object[] {
                c.getId(), c.getCedula(), c.getNombre(),
                c.getTelefono(), c.getEmail()
        };
    }

    // Valores del UPDATE; el id va al final porque es el ? del WHERE.
    @Override
    protected Object[] parametrosActualizar(Cliente c) {
        return new Object[] {
                c.getCedula(), c.getNombre(), c.getTelefono(),
                c.getEmail(), c.getId() // va al final porque en el UPDATE el WHERE es lo último
        };
    }

    // Recibe una fila de MySQL y reconstruye el objeto Cliente.
    @Override
    protected Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(
                rs.getString("id"),
                rs.getString("cedula"),
                rs.getString("nombre"),
                rs.getString("telefono"),
                rs.getString("email"));
    }

    // Busca un cliente por su cédula (además de por id, que ya cubre buscarPorClave).
    // Lo filtra MySQL con un WHERE en vez de recorrer todos los clientes en memoria.
    // Devuelve null si no existe, igual que buscarPorClave.
    public Cliente buscarPorCedula(String cedula) {
        //se llama al metodo consultar de JDBC Support para hacer el trabajo de abrir consulta , select etc
        List<Cliente> resultado = consultar("SELECT * FROM cliente WHERE cedula = ?",
                "buscar el cliente por cédula",
                this::mapear, cedula);
        return resultado.isEmpty() ? null : resultado.get(0); // al final un if por si sale mal o asi , si no esta vacia da el promer elemento
    }
}    //this::mapear es una referencia al método mapear de esta misma clase.
// consultar lo usa como “mapeador”: por cada fila que devuelve MySQL, llama a mapear(rs) para convertir esa fila
// en un objeto Cliente. Es lo mismo que escribir rs -> mapear(rs), solo que más corto.