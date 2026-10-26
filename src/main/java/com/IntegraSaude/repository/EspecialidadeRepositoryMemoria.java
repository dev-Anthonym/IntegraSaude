package com.IntegraSaude.repository;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Especialidade;
import java.util.List;
import java.util.Optional;

/** Especialidades em memória. Simula o UNIQUE(nome). */
public class EspecialidadeRepositoryMemoria implements EspecialidadeRepository {

    private final TabelaMemoria<Especialidade> tabela = new TabelaMemoria<>();

    @Override
    public synchronized Especialidade salvar(Especialidade especialidade) {
        if (tabela.existe(e -> e.getNome().equalsIgnoreCase(especialidade.getNome()))) {
            throw new RegistroDuplicadoException("Especialidade ja cadastrada");
        }
        return tabela.inserir(especialidade);
    }

    @Override
    public Optional<Especialidade> buscarPorId(Long id) {
        return tabela.buscar(id);
    }

    @Override
    public List<Especialidade> listar() {
        return tabela.filtrar(e -> true);
    }
}
