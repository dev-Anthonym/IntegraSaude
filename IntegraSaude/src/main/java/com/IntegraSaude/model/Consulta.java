package com.IntegraSaude.model;

/**
 * A classe Consulta representa os agendamentos de consultas médicas do paciente.
 * Ela armazena informações como médico, especialidade, data e horário.
 */
public class Consulta {

    // --- ATRIBUTOS (CAMPOS) ---
    private Long id;                  // Identificador único da consulta
    private String medico;            // Nome do médico responsável
    private String especialidade;     // Especialidade médica (ex: Cardiologia, Pediatria)
    private String dataHora;          // Data e horário agendados (ex: 2026-11-10 14:30)

    /**
     * Construtor Vazio (Default).
     * Necessário para criar o objeto sem preencher dados imediatamente.
     */
    public Consulta() {}

    /**
     * Construtor Completo com Parâmetros.
     * Facilita a criação da consulta já com todos os dados preenchidos.
     */
    public Consulta(Long id, String medico, String especialidade, String dataHora) {
        this.id = id;
        this.medico = medico;
        this.especialidade = especialidade;
        this.dataHora = dataHora;
    }

    // --- GETTERS E SETTERS ---
    // Métodos para ler (get) e alterar (set) os campos privados da classe.

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMedico() {
        return medico;
    }

    public void setMedico(String medico) {
        this.medico = medico;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void setEspecialidade(String especialidade) {
        this.especialidade = especialidade;
    }

    public String getDataHora() {
        return dataHora;
    }

    public void setDataHora(String dataHora) {
        this.dataHora = dataHora;
    }

    /**
     * Método toString().
     * Formata os dados da consulta em texto para exibições ou logs.
     */
    @Override
    public String toString() {
        return "Consulta [ID=" + id + ", Médico=" + medico + ", Especialidade=" + especialidade + ", Data/Hora=" + dataHora + "]";
    }
}