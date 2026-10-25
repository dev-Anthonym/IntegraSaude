package com.IntegraSaude.repository.jdbc;

import org.sqlite.SQLiteConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe responsável por gerenciar a abertura de conexões JDBC com o banco de dados SQLite.
 * Garante que as restrições de chave estrangeira (PRAGMA foreign_keys = ON) estejam sempre ativas.
 */
public class ConexaoBanco {

    // URL padrão do banco de dados da aplicação
    private static final String URL_PADRAO = "jdbc:sqlite:database/integrasaude.db";

    // URL ativa no momento (permite trocar para banco temporário durante os testes unitários)
    private static String urlAtiva = URL_PADRAO;

    /**
     * Abre uma nova conexão com o banco de dados configurado em urlAtiva.
     *
     * @return Connection ativa com chaves estrangeiras habilitadas.
     */
    public static Connection abrir() {
        try {
            // Configura o driver do SQLite para respeitar integridade referencial (FKs) e evitar bloqueios
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(5000);

            return DriverManager.getConnection(urlAtiva, config.toProperties());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexao com o banco de dados", e);
        }
    }

    /**
     * Abre uma conexão diretamente em um arquivo temporário específico.
     *
     * @param caminhoTemp Caminho do arquivo SQLite temporário.
     * @return Connection isolada para testes.
     */
    public static Connection abrirEmArquivoTemporario(String caminhoTemp) {
        try {
            SQLiteConfig config = new SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(5000);

            return DriverManager.getConnection("jdbc:sqlite:" + caminhoTemp, config.toProperties());
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao abrir conexao temporaria com o banco de dados", e);
        }
    }

    /**
     * Altera a URL de conexão ativa (usado em testes unitários para não sujar o banco principal).
     */
    public static void definirUrlTeste(String caminhoArquivoTemp) {
        urlAtiva = "jdbc:sqlite:" + caminhoArquivoTemp;
    }

    /**
     * Restaura a URL de conexão para o banco de dados padrão do sistema.
     */
    public static void restaurarUrlPadrao() {
        urlAtiva = URL_PADRAO;
    }
}