package com.IntegraSaude.model;

import java.time.LocalDateTime;

// Registro de algo que aconteceu com um lembrete (RN09)
public class EventoHistorico {

    private Long id;
    private final Long lembreteId;
    private final TipoEvento tipoEvento;
    private final LocalDateTime dataHora;

    public EventoHistorico(Long id, Long lembreteId, TipoEvento tipoEvento, LocalDateTime dataHora) {
        this.id = id;
        this.lembreteId = lembreteId;
        this.tipoEvento = tipoEvento;
        this.dataHora = dataHora;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getLembreteId() { return lembreteId; }
    public TipoEvento getTipoEvento() { return tipoEvento; }
    public LocalDateTime getDataHora() { return dataHora; }
}
