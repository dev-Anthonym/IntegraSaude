package com.IntegraSaude.model;

import java.util.Objects;

/**
 * Representa a associação entre um Paciente e um Responsável no sistema.
 */
public class Vinculo {
    
    private Long id;
    private Long pacienteId;
    private Long responsavelId;

    /**
     * Construtor vazio padrão.
     */
    public Vinculo() {
    }

    /**
     * Construtor com todos os atributos da entidade de vínculo.
     */
    public Vinculo(Long id, Long pacienteId, Long responsavelId) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.responsavelId = responsavelId;
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

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    // --- Métodos de Comparação (Equals e HashCode baseados no ID) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vinculo vinculo = (Vinculo) o;
        return Objects.equals(id, vinculo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}