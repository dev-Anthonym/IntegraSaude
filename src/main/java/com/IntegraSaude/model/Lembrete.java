package com.IntegraSaude.model;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import java.time.LocalDateTime;

/**
 * Lembrete de uma dose ou de uma consulta.
 * É a única entidade com regra: a máquina de estados (RN09).
 * PENDENTE -> CONFIRMADO ou PENDENTE -> PERDIDO. Os dois últimos são finais.
 */
public class Lembrete extends EntidadeBase {

    private final Long pacienteId;
    private final TipoLembrete tipo;
    private final Long itemId;
    private final LocalDateTime horarioPrevisto;
    private final String descricao;
    private StatusLembrete status;
    // Espelha a coluna lembrete.alerta_enviado: garante que o responsável é alertado uma única vez (RN11)
    private boolean alertaEnviado;

    // Construtor para lembrete novo: sempre nasce PENDENTE (RN09)
    public Lembrete(Long pacienteId, TipoLembrete tipo, Long itemId,
                    LocalDateTime horarioPrevisto, String descricao) {
        this(pacienteId, tipo, itemId, horarioPrevisto, descricao, StatusLembrete.PENDENTE);
    }

    // Construtor para reconstruir um lembrete já salvo (usado pelos repositórios JDBC)
    public Lembrete(Long pacienteId, TipoLembrete tipo, Long itemId, LocalDateTime horarioPrevisto,
                    String descricao, StatusLembrete status, boolean alertaEnviado) {
        this(pacienteId, tipo, itemId, horarioPrevisto, descricao, status);
        this.alertaEnviado = alertaEnviado;
    }

    private Lembrete(Long pacienteId, TipoLembrete tipo, Long itemId, LocalDateTime horarioPrevisto,
                     String descricao, StatusLembrete status) {
        this.pacienteId = pacienteId;
        this.tipo = tipo;
        this.itemId = itemId;
        this.horarioPrevisto = horarioPrevisto;
        this.descricao = descricao;
        this.status = status;
    }

    // Transição PENDENTE -> CONFIRMADO
    public void confirmar() {
        if (status != StatusLembrete.PENDENTE) {
            throw new OperacaoInvalidaException("Apenas lembretes pendentes podem ser confirmados");
        }
        this.status = StatusLembrete.CONFIRMADO;
    }

    // Transição PENDENTE -> PERDIDO
    public void marcarComoPerdido() {
        if (status != StatusLembrete.PENDENTE) {
            throw new OperacaoInvalidaException("Apenas lembretes pendentes podem ser marcados como perdidos");
        }
        this.status = StatusLembrete.PERDIDO;
    }

    // Marca que o alerta aos responsáveis já foi enviado (só faz sentido para lembrete PERDIDO)
    public void marcarAlertaEnviado() {
        if (status != StatusLembrete.PERDIDO) {
            throw new OperacaoInvalidaException("Apenas lembretes perdidos geram alerta");
        }
        this.alertaEnviado = true;
    }

    public boolean isAlertaEnviado() {
        return alertaEnviado;
    }

    public boolean estaPendente() {
        return status == StatusLembrete.PENDENTE;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public TipoLembrete getTipo() {
        return tipo;
    }

    public Long getItemId() {
        return itemId;
    }

    public LocalDateTime getHorarioPrevisto() {
        return horarioPrevisto;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusLembrete getStatus() {
        return status;
    }
}
