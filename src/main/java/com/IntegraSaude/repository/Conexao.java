package com.IntegraSaude.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A classe Conexao é responsável por gerenciar a ligação com o banco de dados SQLite
 * e garantir que todas as tabelas normalizadas (3FN) do sistema existam.
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
     * Cria todas as tabelas do modelo relacional (3FN) automaticamente caso não existam.
     */
    public static void inicializarBanco() {
        String sqlUsuario = """
            CREATE TABLE IF NOT EXISTS usuario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                email TEXT UNIQUE,
                senha_hash TEXT,
                perfil TEXT,
                data_cadastro TEXT
            );
        """;

        String sqlEspecialidade = """
            CREATE TABLE IF NOT EXISTS especialidade (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL UNIQUE
            );
        """;

        String sqlMedico = """
            CREATE TABLE IF NOT EXISTS medico (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                especialidade_id INTEGER,
                FOREIGN KEY (especialidade_id) REFERENCES especialidade(id)
            );
        """;

        String sqlMedicamento = """
            CREATE TABLE IF NOT EXISTS medicamento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                paciente_id INTEGER,
                nome TEXT NOT NULL,
                dosagem TEXT NOT NULL,
                frequencia_horas INTEGER,
                primeiro_horario TEXT,
                data_inicio TEXT,
                data_fim TEXT,
                FOREIGN KEY (paciente_id) REFERENCES usuario(id)
            );
        """;

        String sqlConsulta = """
            CREATE TABLE IF NOT EXISTS consulta (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                paciente_id INTEGER,
                medico_id INTEGER,
                local TEXT,
                data_hora TEXT,
                status TEXT,
                FOREIGN KEY (paciente_id) REFERENCES usuario(id),
                FOREIGN KEY (medico_id) REFERENCES medico(id)
            );
        """;

        try (Connection conn = conectar();
             Statement stmt = conn.createStatement()) {
            
            // Executa a criação de todas as tabelas normalizadas
            stmt.execute(sqlUsuario);
            stmt.execute(sqlEspecialidade);
            stmt.execute(sqlMedico);
            stmt.execute(sqlMedicamento);
            stmt.execute(sqlConsulta);
            
            // Insere o paciente padrão esperado exatamente pelos repositórios
            stmt.execute("INSERT OR IGNORE INTO usuario (id, nome, email, senha_hash, perfil, data_cadastro) VALUES (1, 'Paciente Padrao', 'paciente.padrao@integrasaude.com', 'HASH', 'PACIENTE', '2026-01-01T00:00:00');");

            System.out.println("-> Banco de dados SQLite inicializado com sucesso!");

        } catch (SQLException e) {
            System.err.println("Erro ao inicializar o banco de dados: " + e.getMessage());
            throw new RuntimeException("Erro ao inicializar banco", e);
        }
    }
}