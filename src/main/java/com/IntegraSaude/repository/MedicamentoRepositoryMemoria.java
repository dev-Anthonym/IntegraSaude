package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medicamento;
import java.util.List;
import java.util.Optional;

/** Medicamentos em memória. */
public class MedicamentoRepositoryMemoria implements MedicamentoRepository {

    private final TabelaMemoria<Medicamento> tabela = new TabelaMemoria<>();

    @Override
    public Medicamento salvar(Medicamento medicamento) {
        return tabela.inserir(medicamento);
    }

    @Override
    public void atualizar(Medicamento medicamento) {
        tabela.substituir(medicamento);
    }

    @Override
    public Optional<Medicamento> buscarPorId(Long id) {
        return tabela.buscar(id);
    }

    @Override
    public List<Medicamento> listarPorPaciente(Long pacienteId) {
        return tabela.filtrar(m -> m.getPacienteId().equals(pacienteId));
    }

    @Override
    public boolean remover(Long id) {
        return tabela.remover(id);
    }
}
