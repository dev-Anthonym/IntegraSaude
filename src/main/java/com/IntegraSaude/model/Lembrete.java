package com.IntegraSaude.model;

import java.time.Clock;
import java.time.Duration;
import com.IntegraSaude.exception.TransicaoInvalidaException;
import java.time.LocalDateTime;

public class Lembrete {

    private Long id;
    private final Long pacienteId;
    private final TipoLembrete tipo;
    private final Long itemId;
    private final String descricao;
    private final LocalDateTime horarioPrevisto;
    private StatusLembrete status;
    private boolean alertaEnviado;

    public Lembrete(Long id, Long pacienteId, TipoLembrete tipo, Long itemId,
                    String descricao, LocalDateTime horarioPrevisto) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.tipo = tipo;
        this.itemId = itemId;
        this.descricao = descricao;
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

    // RN07: o alerta só pode ser registrado uma vez e apenas para lembrete perdido
    public void marcarAlertaEnviado() {
        if (status != StatusLembrete.PERDIDO) {
            throw new TransicaoInvalidaException("Apenas lembrete perdido pode gerar alerta.");
        }
        if (alertaEnviado) {
            throw new TransicaoInvalidaException("O alerta deste lembrete já foi enviado.");
        }
        this.alertaEnviado = true;
    }

    private void exigirPendente() {
        if (status != StatusLembrete.PENDENTE) {
            throw new TransicaoInvalidaException(
                    "Apenas lembrete pendente pode mudar de status. Status atual: " + status);
        }
    }

    public boolean estaVencido(Clock relogio, Duration limite) {
        return estaVencidoEm(LocalDateTime.now(relogio), limite);
    }

    public boolean estaVencidoEm(LocalDateTime agora, Duration limite) {
        return agora.isAfter(horarioPrevisto.plus(limite));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPacienteId() { return pacienteId; }
    public TipoLembrete getTipo() { return tipo; }
    public Long getItemId() { return itemId; }
    public String getDescricao() { return descricao; }
    public LocalDateTime getHorarioPrevisto() { return horarioPrevisto; }
    public StatusLembrete getStatus() { return status; }
    public boolean isAlertaEnviado() { return alertaEnviado; }
}
