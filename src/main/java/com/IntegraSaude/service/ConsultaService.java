package com.IntegraSaude.service;

import com.IntegraSaude.exception.ValidacaoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.repository.ConsultaRepository;
import java.util.List;

/**
 * A classe ConsultaService contém as regras de negócio para a gestão de consultas médicas.
 */
public class ConsultaService {

    private final ConsultaRepository repository;

    public ConsultaService() {
        this.repository = new ConsultaRepository();
    }

    /**
     * Valida os dados e cadastra uma nova consulta.
     */
    public void cadastrarConsulta(String medico, String especialidade, String dataHora) {
        if (medico == null || medico.trim().isEmpty()) {
            throw new ValidacaoException("O nome do médico não pode estar vazio.");
        }
        if (especialidade == null || especialidade.trim().isEmpty()) {
            throw new ValidacaoException("A especialidade médica não pode estar vazia.");
        }
        if (dataHora == null || dataHora.trim().isEmpty()) {
            throw new ValidacaoException("A data e o horário da consulta não podem estar vazios.");
        }

        Consulta consulta = new Consulta(null, medico.trim(), especialidade.trim(), dataHora.trim());
        repository.inserir(consulta);
    }

    /**
     * Retorna a lista de todas as consultas cadastradas.
     */
    public List<Consulta> listarConsultas() {
        return repository.listarTodos();
    }
}