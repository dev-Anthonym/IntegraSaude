package com.IntegraSaude.model;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Medicamento de um paciente (RN05 e RN06).
 * Os horários do dia não ficam aqui: são calculados pela CalculadoraHorarios.
 */
public class Medicamento extends EntidadeBase {

    private final Long pacienteId;
    private final String nome;
    private final String dosagem;
    private final int frequenciaHoras;
    private final LocalTime primeiroHorario;
    private final LocalDate dataInicio;
    private final LocalDate dataFim;

    public Medicamento(Long pacienteId, String nome, String dosagem, int frequenciaHoras,
                       LocalTime primeiroHorario, LocalDate dataInicio, LocalDate dataFim) {
        this.pacienteId = pacienteId;
        this.nome = nome;
        this.dosagem = dosagem;
        this.frequenciaHoras = frequenciaHoras;
        this.primeiroHorario = primeiroHorario;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public String getNome() {
        return nome;
    }

    public String getDosagem() {
        return dosagem;
    }

    public int getFrequenciaHoras() {
        return frequenciaHoras;
    }

    public LocalTime getPrimeiroHorario() {
        return primeiroHorario;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }
}
