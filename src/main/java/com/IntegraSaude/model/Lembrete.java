package com.IntegraSaude.model;

import java.time.LocalDateTime;

public class Lembrete {

    private final Long id;
    private final Long pacienteId;
    private final TipoLembrete tipo;
    private final Long itemId;
    private final LocalDateTime horarioPrevisto;
    private StatusLembrete status;

    public Lembrete(Long id, Long pacienteId, TipoLembrete tipo,
                    Long itemId, LocalDateTime horarioPrevisto) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.tipo = tipo;
        this.itemId = itemId;
        this.horarioPrevisto = horarioPrevisto;
        this.status = StatusLembrete.PENDENTE;
    }

    public void confirmar() {
        this.status = StatusLembrete.CONFIRMADO;
    }

    public void marcarComoPerdido() {
        this.status = StatusLembrete.PERDIDO;
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public TipoLembrete getTipo() { return tipo; }
    public Long getItemId() { return itemId; }
    public LocalDateTime getHorarioPrevisto() { return horarioPrevisto; }
    public StatusLembrete getStatus() { return status; }
}