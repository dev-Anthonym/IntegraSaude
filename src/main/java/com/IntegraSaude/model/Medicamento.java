package com.IntegraSaude.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Representa um medicamento prescrito a um paciente, contendo dados de dosagem e frequência.
 */
public class Medicamento {
    
    private Long id;
    private Long pacienteId;
    private String nome;
    private String dosagem;
    private Integer frequenciaHoras;
    private LocalTime primeiroHorario;
    private LocalDate dataInicio;
    private LocalDate dataFim;

    /**
     * Construtor vazio padrão.
     */
    public Medicamento() {
    }

    /**
     * Construtor com todos os atributos da entidade de medicamento.
     */
    public Medicamento(Long id, Long pacienteId, String nome, String dosagem, 
                       Integer frequenciaHoras, LocalTime primeiroHorario, 
                       LocalDate dataInicio, LocalDate dataFim) {
        this.id = id;
        this.pacienteId = pacienteId;
        this.nome = nome;
        this.dosagem = dosagem;
        this.frequenciaHoras = frequenciaHoras;
        this.primeiroHorario = primeiroHorario;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
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

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDosagem() {
        return dosagem;
    }

    public void setDosagem(String dosagem) {
        this.dosagem = dosagem;
    }

    public Integer getFrequenciaHoras() {
        return frequenciaHoras;
    }

    public void setFrequenciaHoras(Integer frequenciaHoras) {
        this.frequenciaHoras = frequenciaHoras;
    }

    public LocalTime getPrimeiroHorario() {
        return primeiroHorario;
    }

    public void setPrimeiroHorario(LocalTime primeiroHorario) {
        this.primeiroHorario = primeiroHorario;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    // --- Métodos de Comparação (Equals e HashCode baseados no ID) ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medicamento that = (Medicamento) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}