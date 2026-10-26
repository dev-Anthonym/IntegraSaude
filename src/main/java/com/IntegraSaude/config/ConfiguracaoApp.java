package com.IntegraSaude.config;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Configuração da aplicação. Prioridade: argumento de linha de comando, depois modo --demo,
 * depois config.properties, depois o valor padrão.
 *
 * Argumentos aceitos: --demo, --memoria, --tempo-limite=N, --intervalo=N, --banco=caminho/arquivo.db
 */
public record ConfiguracaoApp(ConfiguracaoLembretes lembretes, int intervaloAgendadorSegundos,
                              String caminhoBanco, boolean usarMemoria, boolean carregarDemonstracao) {

    private static final String ARQUIVO_PROPRIEDADES = "/config.properties";
    private static final String PREFIXO_ARGUMENTO = "--";
    // Valores do modo demonstração: lembrete vence em 1 minuto e o agendador roda a cada 10 segundos
    private static final String TEMPO_LIMITE_DEMO = "1";
    private static final String INTERVALO_DEMO = "10";

    // Monta a configuração a partir dos argumentos do main
    public static ConfiguracaoApp carregar(String... args) {
        Properties propriedades = lerPropriedades();
        Map<String, String> argumentos = interpretar(args);
        boolean demo = argumentos.containsKey("demo");

        String tempoLimite = argumentos.getOrDefault("tempo-limite",
                demo ? TEMPO_LIMITE_DEMO : propriedades.getProperty("lembrete.tempo-limite-minutos",
                        String.valueOf(ConfiguracaoLembretes.TEMPO_LIMITE_PADRAO_MINUTOS)));
        String antecedencia = propriedades.getProperty("lembrete.antecedencia-consulta-minutos",
                String.valueOf(ConfiguracaoLembretes.ANTECEDENCIA_CONSULTA_PADRAO_MINUTOS));
        String intervalo = argumentos.getOrDefault("intervalo",
                demo ? INTERVALO_DEMO : propriedades.getProperty("agendador.intervalo-segundos", "30"));
        String banco = argumentos.getOrDefault("banco",
                propriedades.getProperty("banco.caminho", ConexaoBanco.CAMINHO_PADRAO));

        return new ConfiguracaoApp(
                new ConfiguracaoLembretes(inteiro(tempoLimite, "tempo-limite"), inteiro(antecedencia, "antecedencia")),
                inteiro(intervalo, "intervalo"), banco, argumentos.containsKey("memoria"), demo);
    }

    // Transforma "--chave=valor" e "--chave" em um mapa
    private static Map<String, String> interpretar(String[] args) {
        Map<String, String> mapa = new HashMap<>();
        for (String argumento : args) {
            if (!argumento.startsWith(PREFIXO_ARGUMENTO)) {
                throw new DadoInvalidoException("Argumento desconhecido: " + argumento);
            }
            String[] partes = argumento.substring(PREFIXO_ARGUMENTO.length()).split("=", 2);
            mapa.put(partes[0], partes.length > 1 ? partes[1] : "true");
        }
        return mapa;
    }

    // Lê o config.properties do classpath (se não existir, usa só os padrões)
    private static Properties lerPropriedades() {
        Properties propriedades = new Properties();
        try (InputStream entrada = ConfiguracaoApp.class.getResourceAsStream(ARQUIVO_PROPRIEDADES)) {
            if (entrada != null) {
                propriedades.load(entrada);
            }
        } catch (IOException e) {
            throw new DadoInvalidoException("Nao foi possivel ler o config.properties", e);
        }
        return propriedades;
    }

    private static int inteiro(String valor, String nome) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            throw new DadoInvalidoException("Valor invalido para " + nome + ": " + valor, e);
        }
    }
}
