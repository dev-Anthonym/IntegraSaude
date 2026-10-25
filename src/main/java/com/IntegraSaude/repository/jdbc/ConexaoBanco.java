package com.IntegraSaude.repository.jdbc;

import org.sqlite.SQLiteConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexaoBanco {

    private static final String URL = "jdbc:sqlite:database/integrasaude.db";

    public static Connection abrir() {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(5000);

            return DriverManager.getConnection(URL, config.toProperties());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão com o banco de dados", e);
        }
    }

    public static Connection abrirEmArquivoTemporario(String caminhoTemp) {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(5000);

            return DriverManager.getConnection("jdbc:sqlite:" + caminhoTemp, config.toProperties());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexão temporária com o banco de dados", e);
        }
    }
}