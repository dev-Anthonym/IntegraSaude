package com.IntegraSaude.service;

import com.IntegraSaude.exception.ValidacaoException;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.repository.MedicamentoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * A classe MedicamentoService contém as regras de negócio para a gestão de medicamentos.
 */
public class MedicamentoService {

    private final MedicamentoRepository repository;

    public MedicamentoService() {
        this.repository = new MedicamentoRepository();
    }

    /**
     * Valida os dados e cadastra um novo medicamento.
     * @param nome Nome do medicamento
     * @param dosagem Dosagem recomendada
     * @param horarioStr Horário inicial em formato de texto (ex: "08:00")
     * @param frequenciaHoras Intervalo de horas entre as doses
     * @param dataInicioStr Data de início do tratamento (ex: "2026-10-26")
     * @param dataFimStr Data de término do tratamento (ex: "2026-11-02")
     */
    public void cadastrarMedicamento(String nome, String dosagem, String horarioStr, 
                                     Integer frequenciaHoras, String dataInicioStr, String dataFimStr) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new ValidacaoException("O nome do medicamento não pode estar vazio.");
        }
        if (dosagem == null || dosagem.trim().isEmpty()) {
            throw new ValidacaoException("A dosagem do medicamento não pode estar vazia.");
        }
        if (horarioStr == null || horarioStr.trim().isEmpty()) {
            throw new ValidacaoException("O horário do medicamento não pode estar vazio.");
        }

        // Converte o horário de String para LocalTime
        LocalTime primeiroHorario;
        try {
            primeiroHorario = LocalTime.parse(horarioStr.trim());
        } catch (Exception e) {
            throw new ValidacaoException("Formato de horário inválido. Utilize o formato HH:MM.");
        }

        // Converte as datas se forem informadas
        LocalDate dataInicio = (dataInicioStr != null && !dataInicioStr.trim().isEmpty()) 
                ? LocalDate.parse(dataInicioStr.trim()) : LocalDate.now();
        
        LocalDate dataFim = (dataFimStr != null && !dataFimStr.trim().isEmpty()) 
                ? LocalDate.parse(dataFimStr.trim()) : null;

        // Cria a instância de Medicamento com os tipos corretos
        Medicamento medicamento = new Medicamento(
                null, 
                null, // pacienteId 
                nome.trim(), 
                dosagem.trim(), 
                frequenciaHoras != null ? frequenciaHoras : 8, // Valor padrão de frequência se nulo
                primeiroHorario, 
                dataInicio, 
                dataFim
        );

        repository.inserir(medicamento);
        System.out.println("-> Medicamento registado com sucesso na camada de serviço!");
    }

    /**
     * Retorna a lista de todos os medicamentos cadastrados.
     * @return Lista de medicamentos
     */
    public List<Medicamento> listarMedicamentos() {
        return repository.listarTodos();
    }
}