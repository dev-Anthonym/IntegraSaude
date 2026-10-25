package com.IntegraSaude.model;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa um lembrete gerado no sistema (seja para medicamento ou consulta).
 */
public class Lembrete {
    
    private Long id;
    private Long pacienteId;
    private TipoLembrete tipo;
    private Long itemId; // ID do medicamento ou da consulta correspondente
    private LocalDateTime horarioPrevisto;
    private StatusLembrete status;

    /**
     * Construtor vazio padrão (por defeito nasce com estado PENDENTE).
     */
    public Lembrete() {
        this.status = StatusLembrete.PENDENTE;
    }

    /**
     * Construtor com os atributos principais do lembrete.
     */
    public Lembrete(Long id, Long pacienteId, TipoLembrete tipo, Long itemId, LocalDateTime horarioPrevisto) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.tipo = tipo;
        this.itemId = itemId;
        this.horarioPrevisto = horarioPrevisto;
        this.status = StatusLembrete.PENDENTE; // Todo lembrete nasce PENDENTE
    }

    // --- Máquina de Estados (Transições) ---

    /**
     * Confirma o lembrete se o mesmo estiver pendente.
     */
    public void confirmar() {
        if (this.status != StatusLembrete.PENDENTE) {
            throw new OperacaoInvalidaException("Apenas lembretes pendentes podem ser confirmados.");
        }
        this.status = StatusLembrete.CONFIRMADO;
    }

    /**
     * Marca o lembrete como perdido caso ultrapasse o tempo limite sem confirmação.
     */
    public void marcarComoPerdido() {
        if (this.status != StatusLembrete.PENDENTE) {
            throw new OperacaoInvalidaException("Apenas lembretes pendentes podem ser marcados como perdidos.");
        }
        this.status = StatusLembrete.PERDIDO;
    }

    // --- Getters e Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(Long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public TipoLembrete getTipo() {
        return tipo;
    }

    public void setTipo(TipoLembrete tipo) {
        this.tipo = tipo;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public LocalDateTime getHorarioPrevisto() {
        return horarioPrevisto;
    }

    public void setHorarioPrevisto(LocalDateTime horarioPrevisto) {
        this.horarioPrevisto = horarioPrevisto;
    }

    public StatusLembrete getStatus() {
        return status;
    }

    public void setStatus(StatusLembrete status) {
        this.status = status;
    }

    // --- Métodos de Comparação (Equals e HashCode baseados no ID) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lembrete lembrete = (Lembrete) o;
        return Objects.equals(id, lembrete.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}