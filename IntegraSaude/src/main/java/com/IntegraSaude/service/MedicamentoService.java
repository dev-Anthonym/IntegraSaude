package com.IntegraSaude.service;

import com.IntegraSaude.exception.ValidacaoException;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.repository.MedicamentoRepository;
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
     */
    public void cadastrarMedicamento(String nome, String dosagem, String horario) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new ValidacaoException("O nome do medicamento não pode estar vazio.");
        }
        if (dosagem == null || dosagem.trim().isEmpty()) {
            throw new ValidacaoException("A dosagem do medicamento não pode estar vazia.");
        }
        if (horario == null || horario.trim().isEmpty()) {
            throw new ValidacaoException("O horário do medicamento não pode estar vazio.");
        }

        // Correção aplicada aqui: passado apenas null sem o rótulo "id:"
        Medicamento medicamento = new Medicamento(null, nome.trim(), dosagem.trim(), horario.trim());
        repository.inserir(medicamento);
    }

    /**
     * Retorna a lista de todos os medicamentos cadastrados.
     */
    public List<Medicamento> listarMedicamentos() {
        return repository.listarTodos();
    }
}