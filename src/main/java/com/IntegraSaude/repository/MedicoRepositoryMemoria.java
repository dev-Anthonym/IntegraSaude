package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medico;
import java.util.List;
import java.util.Optional;

/** Médicos em memória. */
public class MedicoRepositoryMemoria implements MedicoRepository {

    private final TabelaMemoria<Medico> tabela = new TabelaMemoria<>();

    @Override
    public Medico salvar(Medico medico) {
        return tabela.inserir(medico);
    }

    @Override
    public Optional<Medico> buscarPorId(Long id) {
        return tabela.buscar(id);
    }

    @Override
    public List<Medico> listarPorEspecialidade(Long especialidadeId) {
        return tabela.filtrar(m -> m.getEspecialidade().getId().equals(especialidadeId));
    }
}
