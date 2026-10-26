package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Consulta;
import java.time.Clock;
import java.time.LocalDateTime;

/** Valida os dados de uma consulta (RN08). Recebe o Clock para ser testável. */
public class ValidadorConsulta {

    private final Clock relogio;

    public ValidadorConsulta(Clock relogio) {
        this.relogio = relogio;
    }

    // Roda todas as validações; a primeira que falhar lança DadoInvalidoException
    public void validar(Consulta consulta) {
        // Médico (e, por consequência, a especialidade) obrigatório
        if (consulta.getMedico() == null || consulta.getMedico().getEspecialidade() == null) {
            throw new DadoInvalidoException("A especialidade e o medico sao obrigatorios");
        }
        // Local obrigatório
        if (consulta.getLocal() == null || consulta.getLocal().isBlank()) {
            throw new DadoInvalidoException("O local da consulta e obrigatorio");
        }
        // Data e hora obrigatórias e não podem estar no passado
        if (consulta.getDataHora() == null) {
            throw new DadoInvalidoException("A data e a hora da consulta sao obrigatorias");
        }
        if (consulta.getDataHora().isBefore(LocalDateTime.now(relogio))) {
            throw new DadoInvalidoException("A consulta nao pode estar no passado");
        }
    }
}
