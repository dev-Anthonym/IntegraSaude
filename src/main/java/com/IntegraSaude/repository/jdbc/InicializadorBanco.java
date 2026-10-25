package com.IntegraSaude.repository.jdbc;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public class InicializadorBanco {

    private static final String[] SCRIPTS = {
        "/db/01_usuarios.sql",
        "/db/02_medicamentos.sql",
        "/db/03_consultas.sql",
        "/db/04_lembretes.sql"
    };

    public static void executar(Connection conexao) {
        for (String caminhoScript : SCRIPTS) {
            executarScript(conexao, caminhoScript);
        }
        System.out.println("-> Inicializador do banco executado com sucesso!");
    }

    private static void executarScript(Connection conexao, String caminhoResource) {
        try (InputStream is = InicializadorBanco.class.getResourceAsStream(caminhoResource)) {
            if (is == null) {
                throw new RuntimeException("Script não encontrado no classpath: " + caminhoResource);
            }

            String conteudoSql;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                conteudoSql = reader.lines()
                    .filter(linha -> !linha.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
            }

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