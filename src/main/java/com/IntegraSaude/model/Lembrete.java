package com.IntegraSaude.model;

import java.time.Clock;
import java.time.Duration;
import com.IntegraSaude.exception.TransicaoInvalidaException;
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
        exigirPendente();
        this.status = StatusLembrete.CONFIRMADO;
    }

    public void marcarComoPerdido() {
        exigirPendente();
        this.status = StatusLembrete.PERDIDO;
    }

    private void exigirPendente() {
        if (status != StatusLembrete.PENDENTE) {
            throw new TransicaoInvalidaException(
                    "Apenas lembrete pendente pode mudar de status. Status atual: " + status);
        }
    }

    public boolean estaVencido(Clock relogio, Duration limite) {
        LocalDateTime agora = LocalDateTime.now(relogio);
        return agora.isAfter(horarioPrevisto.plus(limite));
    }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public TipoLembrete getTipo() { return tipo; }
    public Long getItemId() { return itemId; }
    public LocalDateTime getHorarioPrevisto() { return horarioPrevisto; }
    public StatusLembrete getStatus() { return status; }
}