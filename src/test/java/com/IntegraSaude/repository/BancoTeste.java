package com.IntegraSaude.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

// Cria um banco H2 em memória novo, com as tabelas do módulo de lembretes
final class BancoTeste {

    private static final String DDL_LEMBRETES = "database/04_lembretes.sql";
    private static final String DDL_DEPENDENCIAS = "classpath:database/dependencias_lembretes.sql";

    private BancoTeste() {
    }

    static FabricaConexao criar() throws SQLException {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        FabricaConexao fabrica = () -> DriverManager.getConnection(url);
        executarScript(fabrica, DDL_DEPENDENCIAS);
        executarScript(fabrica, DDL_LEMBRETES);
        return fabrica;
    }

    static void executarScript(FabricaConexao fabrica, String caminho) throws SQLException {
        try (Connection conexao = fabrica.abrir(); Statement comando = conexao.createStatement()) {
            comando.execute("RUNSCRIPT FROM '" + caminho + "'");
        }
    }
}
