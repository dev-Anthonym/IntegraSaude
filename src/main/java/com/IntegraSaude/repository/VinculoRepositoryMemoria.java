package com.IntegraSaude.repository;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Vinculo;
import java.util.List;
import java.util.Optional;

/** Vínculos em memória. Simula o UNIQUE(paciente, responsavel). */
public class VinculoRepositoryMemoria implements VinculoRepository {

    private final TabelaMemoria<Vinculo> tabela = new TabelaMemoria<>();

    @Override
    public synchronized Vinculo salvar(Vinculo vinculo) {
        if (tabela.existe(v -> v.getPacienteId().equals(vinculo.getPacienteId())
                && v.getResponsavelId().equals(vinculo.getResponsavelId()))) {
            throw new RegistroDuplicadoException("Vinculo ja existe");
        }
        return tabela.inserir(vinculo);
    }

    @Override
    public Optional<Vinculo> buscarPorPar(Long pacienteId, Long responsavelId) {
        return tabela.filtrar(v -> v.getPacienteId().equals(pacienteId)
                && v.getResponsavelId().equals(responsavelId)).stream().findFirst();
    }

    @Override
    public List<Vinculo> listarPorPaciente(Long pacienteId) {
        return tabela.filtrar(v -> v.getPacienteId().equals(pacienteId));
    }

    @Override
    public List<Vinculo> listarPorResponsavel(Long responsavelId) {
        return tabela.filtrar(v -> v.getResponsavelId().equals(responsavelId));
    }

    @Override
    public boolean remover(Long id) {
        return tabela.remover(id);
    }
}
