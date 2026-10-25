package com.IntegraSaude.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A classe Conexao é responsável por gerenciar a ligação com o banco de dados SQLite
 * e garantir que as tabelas necessárias existam no sistema.
 */
public class Conexao {

    // Caminho do arquivo do banco de dados SQLite
    private static final String URL_BANCO = "jdbc:sqlite:database/integrasaude.db";

    /**
     * Estabelece e retorna uma nova conexão com o banco de dados.
     * @return Connection objeto de conexão JDBC
     * @throws SQLException se houver erro ao conectar
     */
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL_BANCO);
    }

    /**
     * Cria as tabelas do sistema automaticamente caso elas ainda não existam.
     */
    public static void inicializarBanco() {
        String sqlMedicamentos = """
            CREATE TABLE IF NOT EXISTS medicamentos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                dosagem TEXT NOT NULL,
                horario TEXT NOT NULL
            );
        """;

        String sqlConsultas = """
            CREATE TABLE IF NOT EXISTS consultas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                medico TEXT NOT NULL,
                especialidade TEXT NOT NULL,
                data_hora TEXT NOT NULL
            );
        """;

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement()) {
            
            // Executa a criação das tabelas
            stmt.execute(sqlMedicamentos);
            stmt.execute(sqlConsultas);
            
            System.out.println("-> Banco de dados SQLite inicializado com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
        }
    }
}