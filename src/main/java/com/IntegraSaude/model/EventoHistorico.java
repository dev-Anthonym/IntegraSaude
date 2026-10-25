package com.IntegraSaude.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Regista eventos do sistema para efeitos de histórico e auditoria (RN13).
 */
public class EventoHistorico {
    
    private Long id;
    private Long pacienteId;
    private TipoEvento tipoEvento;
    private String descricao;
    private LocalDateTime dataHora;

    /**
     * Construtor vazio padrão.
     */
    public EventoHistorico() {
    }

    /**
     * Construtor com todos os atributos do evento histórico.
     */
    public EventoHistorico(Long id, Long pacienteId, TipoEvento tipoEvento, String descricao, LocalDateTime dataHora) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.tipoEvento = tipoEvento;
        this.descricao = descricao;
        this.dataHora = dataHora;
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

    public TipoEvento getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(TipoEvento tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    // --- Métodos de Comparação (Equals e HashCode baseados no ID) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventoHistorico that = (EventoHistorico) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}