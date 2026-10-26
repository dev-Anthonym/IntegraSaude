package com.IntegraSaude.repository;

import com.IntegraSaude.model.Especialidade;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de especialidades médicas. */
public interface EspecialidadeRepository {

    // Nome repetido (ignorando maiúsculas) lança RegistroDuplicadoException
    Especialidade salvar(Especialidade especialidade);

    Optional<Especialidade> buscarPorId(Long id);

    List<Especialidade> listar();
}
