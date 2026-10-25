package com.IntegraSaude.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa uma consulta médica agendada para um paciente.
 */
public class Consulta {
    
    private Long id;
    private Long pacienteId;
    private String especialidade;
    private String medico;
    private String local;
    private LocalDateTime dataHora;

    /**
     * Construtor vazio padrão.
     */
    public Consulta() {
    }

    /**
     * Construtor com todos os atributos da entidade de consulta.
     */
    public Consulta(Long id, Long pacienteId, String especialidade, String medico, String local, LocalDateTime dataHora) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.especialidade = especialidade;
        this.medico = medico;
        this.local = local;
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

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public String getMedico() {
        return medico;
    }

    public void setMedico(String medico) {
        this.medico = medico;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
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
        Consulta consulta = (Consulta) o;
        return Objects.equals(id, consulta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}