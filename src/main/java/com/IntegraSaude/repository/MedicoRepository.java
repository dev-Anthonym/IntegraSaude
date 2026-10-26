package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medico;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de médicos. */
public interface MedicoRepository {

    Medico salvar(Medico medico);

    Optional<Medico> buscarPorId(Long id);

    List<Medico> listarPorEspecialidade(Long especialidadeId);
}
