package com.IntegraSaude.model;

import java.time.LocalDateTime;

/** Registro do histórico (RN13): o que aconteceu, com quem e quando. */
public class EventoHistorico extends EntidadeBase {

    private final Long pacienteId;
    private final Long lembreteId;
    private final TipoEvento tipo;
    private final LocalDateTime dataHora;
    private final String descricao;

    public EventoHistorico(Long pacienteId, Long lembreteId, TipoEvento tipo,
                           LocalDateTime dataHora, String descricao) {
        this.pacienteId = pacienteId;
        this.lembreteId = lembreteId;
        this.tipo = tipo;
        this.dataHora = dataHora;
        this.descricao = descricao;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Long getLembreteId() {
        return lembreteId;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDescricao() {
        return descricao;
    }
}
