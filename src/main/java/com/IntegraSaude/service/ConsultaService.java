package com.IntegraSaude.service;

import com.IntegraSaude.exception.ValidacaoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.repository.ConsultaRepository;
import java.time.LocalDateTime;
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
     * @param medico Nome do médico
     * @param especialidade Especialidade médica
     * @param dataHoraStr Data e hora no formato de texto (ex: ISO-8601 ou yyyy-MM-ddTHH:mm)
     */
    public void cadastrarConsulta(String medico, String especialidade, String dataHoraStr) {
        if (medico == null || medico.trim().isEmpty()) {
            throw new ValidacaoException("O nome do médico não pode estar vazio.");
        }
        if (especialidade == null || especialidade.trim().isEmpty()) {
            throw new ValidacaoException("A especialidade médica não pode estar vazia.");
        }
        if (dataHoraStr == null || dataHoraStr.trim().isEmpty()) {
            throw new ValidacaoException("A data e o horário da consulta não podem estar vazios.");
        }

        // Converte a String recebida para LocalDateTime
        LocalDateTime dataHora;
        try {
            dataHora = LocalDateTime.parse(dataHoraStr.trim());
        } catch (Exception e) {
            throw new ValidacaoException("Formato de data e hora inválido. Utilize o formato correto (ex: AAAA-MM-DDTHH:MM).");
        }

        // Cria a instância de Consulta com o LocalDateTime convertido
        Consulta consulta = new Consulta(null, null, especialidade.trim(), medico.trim(), null, dataHora);
        repository.inserir(consulta);
    }

    /**
     * Retorna a lista de todas as consultas cadastradas.
     * @return Lista de consultas
     */
    public List<Consulta> listarConsultas() {
        return repository.listarTodos();
    }
}