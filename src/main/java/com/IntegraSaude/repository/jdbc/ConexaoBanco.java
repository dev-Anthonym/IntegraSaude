package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.exception.PersistenciaException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Abre conexões JDBC com o SQLite. Toda conexão sai com as chaves estrangeiras ligadas
 * (no SQLite elas vêm desligadas por padrão) e com tempo de espera contra bloqueios.
 */
public final class ConexaoBanco {

    // Arquivo padrão do banco (a pasta database/ já existe no repositório; o *.db é ignorado pelo Git)
    public static final String CAMINHO_PADRAO = "database/integrasaude.db";

    private static final String PREFIXO_URL = "jdbc:sqlite:";
    private static final int TEMPO_ESPERA_MS = 5000;

    // URL ativa no momento (permite apontar para um banco temporário nos testes)
    private static String urlAtiva = PREFIXO_URL + CAMINHO_PADRAO;

    private ConexaoBanco() {
    }

    // Abre uma conexão com a URL ativa (banco padrão, ou o de teste se definirUrlTeste foi chamado)
    public static Connection abrir() {
        return abrirUrl(urlAtiva);
    }

    // Abre (ou cria) o arquivo do banco no caminho informado
    public static Connection abrir(String caminho) {
        criarPastaDoArquivo(caminho);
        return abrirUrl(PREFIXO_URL + caminho);
    }

    // Abre uma conexão em um arquivo temporário (uso nos testes)
    public static Connection abrirEmArquivoTemporario(String caminhoTemp) {
        return abrir(caminhoTemp);
    }

    // Troca a URL ativa para um banco de teste, sem sujar o banco principal
    public static void definirUrlTeste(String caminhoArquivoTemp) {
        urlAtiva = PREFIXO_URL + caminhoArquivoTemp;
    }

    // Volta para o banco padrão
    public static void restaurarUrlPadrao() {
        urlAtiva = PREFIXO_URL + CAMINHO_PADRAO;
    }

    // Conecta e liga as chaves estrangeiras
    private static Connection abrirUrl(String url) {
        try {
            Connection conexao = DriverManager.getConnection(url);
            try (Statement comando = conexao.createStatement()) {
                comando.execute("PRAGMA foreign_keys = ON");
                comando.execute("PRAGMA busy_timeout = " + TEMPO_ESPERA_MS);
            }
            return conexao;
        } catch (SQLException e) {
            throw new PersistenciaException("Nao foi possivel abrir o banco de dados", e);
        }
    }

    // O SQLite cria o arquivo, mas não cria a pasta: se ela não existir, cria
    private static void criarPastaDoArquivo(String caminho) {
        Path pasta = Path.of(caminho).toAbsolutePath().getParent();
        try {
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel criar a pasta do banco de dados", e);
        }
    }
}
