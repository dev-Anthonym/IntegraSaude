package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Medicamento;
import java.time.Clock;
import java.time.LocalDate;

/** Valida os dados de um medicamento (RN05). Recebe o Clock para ser testável. */
public class ValidadorMedicamento {

    // Limites da frequência em horas
    public static final int FREQUENCIA_MINIMA_HORAS = 1;
    public static final int FREQUENCIA_MAXIMA_HORAS = 24;

    private final Clock relogio;

    public ValidadorMedicamento(Clock relogio) {
        this.relogio = relogio;
    }

    // Roda todas as validações; a primeira que falhar lança DadoInvalidoException
    public void validar(Medicamento medicamento) {
        validarTextos(medicamento);
        validarFrequencia(medicamento.getFrequenciaHoras());
        validarPeriodo(medicamento);
    }

    // Nome e dosagem obrigatórios
    private void validarTextos(Medicamento medicamento) {
        if (medicamento.getNome() == null || medicamento.getNome().isBlank()) {
            throw new DadoInvalidoException("O nome do medicamento e obrigatorio");
        }
        if (medicamento.getDosagem() == null || medicamento.getDosagem().isBlank()) {
            throw new DadoInvalidoException("A dosagem e obrigatoria");
        }
    }

    // Frequência entre 1 e 24 horas
    private void validarFrequencia(int frequenciaHoras) {
        if (frequenciaHoras < FREQUENCIA_MINIMA_HORAS || frequenciaHoras > FREQUENCIA_MAXIMA_HORAS) {
            throw new DadoInvalidoException("A frequencia deve estar entre "
                    + FREQUENCIA_MINIMA_HORAS + " e " + FREQUENCIA_MAXIMA_HORAS + " horas");
        }
    }

    // Horário inicial obrigatório, início <= fim e fim não pode estar no passado
    private void validarPeriodo(Medicamento medicamento) {
        if (medicamento.getPrimeiroHorario() == null) {
            throw new DadoInvalidoException("O horario inicial e obrigatorio");
        }
        if (medicamento.getDataInicio() == null || medicamento.getDataFim() == null) {
            throw new DadoInvalidoException("As datas de inicio e fim sao obrigatorias");
        }
        if (medicamento.getDataInicio().isAfter(medicamento.getDataFim())) {
            throw new DadoInvalidoException("A data de inicio nao pode ser posterior a data de fim");
        }
        if (medicamento.getDataFim().isBefore(LocalDate.now(relogio))) {
            throw new DadoInvalidoException("A data de fim nao pode estar no passado");
        }
    }
}
