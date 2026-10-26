package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de consultas. */
public interface ConsultaRepository {

    // Mesmo paciente no mesmo horário (entre consultas AGENDADAS) lança RegistroDuplicadoException
    Consulta salvar(Consulta consulta);

    void atualizar(Consulta consulta);

    Optional<Consulta> buscarPorId(Long id);

    // Só considera consultas AGENDADAS (cancelada libera o horário)
    Optional<Consulta> buscarPorPacienteEHorario(Long pacienteId, LocalDateTime dataHora);

    // Consultas do paciente, exceto as CANCELADAS
    List<Consulta> listarPorPaciente(Long pacienteId);
}
