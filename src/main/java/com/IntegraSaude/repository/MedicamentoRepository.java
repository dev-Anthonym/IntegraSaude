package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medicamento;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de medicamentos. */
public interface MedicamentoRepository {

    Medicamento salvar(Medicamento medicamento);

    // Atualiza pelo id do objeto recebido
    void atualizar(Medicamento medicamento);

    Optional<Medicamento> buscarPorId(Long id);

    List<Medicamento> listarPorPaciente(Long pacienteId);

    boolean remover(Long id);
}
