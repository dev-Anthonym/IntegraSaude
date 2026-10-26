package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;

/**
 * Parâmetros configuráveis dos lembretes.
 * Na demonstração o tempo limite cai de 30 para 1 minuto (RN10).
 */
public record ConfiguracaoLembretes(int tempoLimiteMinutos, int antecedenciaConsultaMinutos) {

    public static final int TEMPO_LIMITE_PADRAO_MINUTOS = 30;
    public static final int ANTECEDENCIA_CONSULTA_PADRAO_MINUTOS = 60;

    // Construtor compacto: valida os valores sempre que o record é criado
    public ConfiguracaoLembretes {
        if (tempoLimiteMinutos <= 0) {
            throw new DadoInvalidoException("O tempo limite do lembrete deve ser maior que zero");
        }
        if (antecedenciaConsultaMinutos < 0) {
            throw new DadoInvalidoException("A antecedencia da consulta nao pode ser negativa");
        }
    }

    // Configuração com os valores padrão (30 e 60 minutos)
    public static ConfiguracaoLembretes padrao() {
        return new ConfiguracaoLembretes(TEMPO_LIMITE_PADRAO_MINUTOS, ANTECEDENCIA_CONSULTA_PADRAO_MINUTOS);
    }
}
