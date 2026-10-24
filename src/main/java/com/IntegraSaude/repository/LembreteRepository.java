package com.IntegraSaude.repository;

import com.IntegraSaude.model.Lembrete;
import java.util.List;
import java.util.Optional;

// Contrato de persistência dos lembretes de medicamentos e consultas
public interface LembreteRepository {
    Lembrete salvar(Lembrete lembrete);
    Optional<Lembrete> buscarPorId(Long id);
    List<Lembrete> listarPendentesDoPaciente(Long pacienteId);
    List<Lembrete> listarPorPaciente(Long pacienteId);
    List<Lembrete> listarPendentes();
    void atualizar(Lembrete lembrete);
}
