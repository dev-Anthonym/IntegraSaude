package com.IntegraSaude.repository.jdbc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Classe responsável por inicializar o esquema do banco de dados SQLite,
 * lendo e executando automaticamente os scripts DDL contidos no classpath (/db/).
 */
public class InicializadorBanco {

    // Lista ordenada dos scripts DDL que criam as 8 tabelas do sistema na 3FN
    private static final String[] SCRIPTS = {
        "/db/01_usuarios.sql",
        "/db/02_medicamentos.sql",
        "/db/03_consultas.sql",
        "/db/04_lembretes.sql"
    };

    /**
     * Executa todos os scripts DDL em sequência utilizando a conexão informada.
     *
     * @param conexao Conexão JDBC aberta com o SQLite.
     */
    public static void executar(Connection conexao) {
        for (String caminhoScript : SCRIPTS) {
            executarScript(conexao, caminhoScript);
        }
    }

    /**
     * Lê um arquivo .sql do classpath, remove comentários de linha e executa cada comando SQL.
     */
    private static void executarScript(Connection conexao, String caminhoResource) {
        try (InputStream is = InicializadorBanco.class.getResourceAsStream(caminhoResource)) {
            if (is == null) {
                throw new RuntimeException("Script nao encontrado no classpath: " + caminhoResource);
            }

            // Lê todo o conteúdo do arquivo ignorando linhas de comentário SQL (--)
            String conteudoSql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                conteudoSql = reader.lines()
                    .filter(linha -> !linha.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            }

            // Divide o script pelo ponto e vírgula (;) e executa cada instrução individualmente
            String[] comandos = conteudoSql.split(";");
            try (Statement stmt = conexao.createStatement()) {
                for (String comando : comandos) {
                    if (!comando.trim().isEmpty()) {
                        stmt.execute(comando);
                    }
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao executar o script SQL: " + caminhoResource, e);
        }
    }
}