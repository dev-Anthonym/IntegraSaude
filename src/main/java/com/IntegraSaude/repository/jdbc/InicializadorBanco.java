package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.exception.PersistenciaException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Cria e atualiza as tabelas lendo os scripts SQL do projeto (pasta database/,
 * copiada para db/ no classpath pelo Maven). Pode rodar a cada abertura do banco.
 */
public final class InicializadorBanco {

    // Ordem importa: as tabelas dependem umas das outras por chave estrangeira.
    // O 05 altera tabelas já criadas (ex.: telefone do usuário).
    private static final List<String> SCRIPTS = List.of(
            "01_usuarios.sql", "02_medicamentos.sql", "03_consultas.sql",
            "04_lembretes.sql", "05_alteracoes.sql");
    private static final String PASTA = "/db/";
    private static final String PREFIXO_COMENTARIO = "--";
    // ALTER TABLE ADD COLUMN não tem IF NOT EXISTS: na segunda execução o SQLite avisa que a coluna já existe
    private static final String COLUNA_JA_EXISTE = "duplicate column name";

    private InicializadorBanco() {
    }

    // Executa todos os scripts de estrutura em ordem
    public static void executar(Connection conexao) {
        for (String script : SCRIPTS) {
            executarScript(conexao, script);
        }
    }

    // Executa um script do classpath (também usado para carregar os dados de demonstração nos testes)
    public static void executarScript(Connection conexao, String nomeScript) {
        for (String comando : separarComandos(lerScript(nomeScript))) {
            executarComando(conexao, comando);
        }
    }

    // Lê o script do classpath como texto UTF-8
    private static String lerScript(String nome) {
        try (InputStream entrada = InicializadorBanco.class.getResourceAsStream(PASTA + nome)) {
            if (entrada == null) {
                throw new PersistenciaException("Script SQL nao encontrado: " + nome);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Nao foi possivel ler o script " + nome, e);
        }
    }

    // Remove comentários de linha e separa os comandos pelo ponto e vírgula
    private static List<String> separarComandos(String conteudo) {
        StringBuilder semComentarios = new StringBuilder();
        for (String linha : conteudo.split("\n")) {
            if (!linha.trim().startsWith(PREFIXO_COMENTARIO)) {
                semComentarios.append(linha).append('\n');
            }
        }
        return Arrays.stream(semComentarios.toString().split(";"))
                .map(String::trim)
                .filter(comando -> !comando.isEmpty())
                .toList();
    }

    // Executa um comando; ignora só o aviso de "coluna já existe" (script de alteração rodando de novo)
    private static void executarComando(Connection conexao, String comando) {
        try (Statement declaracao = conexao.createStatement()) {
            declaracao.execute(comando);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains(COLUNA_JA_EXISTE)) {
                return;
            }
            throw new PersistenciaException("Falha ao executar o script SQL: " + comando, e);
        }
    }
}
