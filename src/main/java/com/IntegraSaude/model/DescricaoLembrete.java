package com.IntegraSaude.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Monta os textos mostrados ao usuário. O banco não guarda descrição de lembrete nem de evento:
 * o texto é sempre calculado a partir dos dados do medicamento/consulta. Centralizar aqui
 * garante o mesmo texto com repositório em memória ou SQLite.
 */
public final class DescricaoLembrete {

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private DescricaoLembrete() {
    }

    // Ex.: "Tomar Dipirona (500 mg)"
    public static String medicamento(String nome, String dosagem) {
        return "Tomar " + nome + " (" + dosagem + ")";
    }

    // Ex.: "Consulta de Cardiologia com Dr. Carlos em Clinica Central as 14:00"
    public static String consulta(String especialidade, String medico, String local, LocalDateTime dataHora) {
        return "Consulta de " + especialidade + " com " + medico + " em " + local
                + " as " + dataHora.format(FORMATO_HORA);
    }

    // Ex.: "Perdido: Tomar Dipirona (500 mg)"
    public static String evento(TipoEvento tipo, String descricaoLembrete) {
        String rotulo = switch (tipo) {
            case GERADO -> "Gerado";
            case CONFIRMADO -> "Confirmado";
            case PERDIDO -> "Perdido";
            case ALERTA_ENVIADO -> "Alerta enviado";
        };
        return rotulo + ": " + descricaoLembrete;
    }
}
