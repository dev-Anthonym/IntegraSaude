package com.IntegraSaude.repository;

import com.IntegraSaude.model.Vinculo;
import java.util.List;
import java.util.Optional;

/** Acesso a dados dos vínculos paciente-responsável. */
public interface VinculoRepository {

    // Insere o vínculo. Par repetido lança RegistroDuplicadoException
    Vinculo salvar(Vinculo vinculo);

    Optional<Vinculo> buscarPorPar(Long pacienteId, Long responsavelId);

    List<Vinculo> listarPorPaciente(Long pacienteId);

    List<Vinculo> listarPorResponsavel(Long responsavelId);

    // Devolve true se removeu alguma linha
    boolean remover(Long id);
}
