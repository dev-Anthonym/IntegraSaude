package com.IntegraSaude.repository;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.StatusConsulta;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Consultas em memória. Simula o índice único (paciente, data_hora) válido só para consultas AGENDADAS. */
public class ConsultaRepositoryMemoria implements ConsultaRepository {

    private final TabelaMemoria<Consulta> tabela = new TabelaMemoria<>();

    @Override
    public synchronized Consulta salvar(Consulta consulta) {
        if (consulta.estaAgendada() && tabela.existe(c -> c.estaAgendada()
                && c.getPacienteId().equals(consulta.getPacienteId())
                && c.getDataHora().equals(consulta.getDataHora()))) {
            throw new RegistroDuplicadoException("Ja existe consulta neste horario");
        }
        return tabela.inserir(consulta);
    }

    @Override
    public synchronized void atualizar(Consulta consulta) {
        if (consulta.estaAgendada() && tabela.existe(c -> c.estaAgendada() && !c.getId().equals(consulta.getId())
                && c.getPacienteId().equals(consulta.getPacienteId())
                && c.getDataHora().equals(consulta.getDataHora()))) {
            throw new RegistroDuplicadoException("Ja existe consulta neste horario");
        }
        tabela.substituir(consulta);
    }

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        return tabela.buscar(id);
    }

    @Override
    public Optional<Consulta> buscarPorPacienteEHorario(Long pacienteId, LocalDateTime dataHora) {
        return tabela.filtrar(c -> c.estaAgendada() && c.getPacienteId().equals(pacienteId)
                && c.getDataHora().equals(dataHora)).stream().findFirst();
    }

    @Override
    public List<Consulta> listarPorPaciente(Long pacienteId) {
        return tabela.filtrar(c -> c.getStatus() != StatusConsulta.CANCELADA
                && c.getPacienteId().equals(pacienteId));
    }
}
