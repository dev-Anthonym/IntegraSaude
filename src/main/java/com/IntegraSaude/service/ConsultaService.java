package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.ConsultaRepository;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Cadastro e manutenção de consultas (RN06 e RN08). */
public class ConsultaService {

    private final ConsultaRepository consultas;
    private final UsuarioRepository usuarios;
    private final LembreteRepository lembretes;
    private final ValidadorConsulta validador;
    private final Clock relogio;

    public ConsultaService(ConsultaRepository consultas, UsuarioRepository usuarios,
                           LembreteRepository lembretes, ValidadorConsulta validador, Clock relogio) {
        this.consultas = consultas;
        this.usuarios = usuarios;
        this.lembretes = lembretes;
        this.validador = validador;
        this.relogio = relogio;
    }

    // Cadastra: dono PACIENTE, dados válidos e sem conflito de horário
    public Consulta cadastrar(Consulta consulta) {
        exigirPaciente(consulta.getPacienteId());
        validador.validar(consulta);
        validarConflito(consulta, null);
        return consultas.salvar(consulta);
    }

    // Atualiza: só consulta agendada e que ainda não passou pode ser editada (RN08)
    public void atualizar(Consulta nova) {
        Consulta atual = buscarDoPaciente(nova.getPacienteId(), nova.getId());
        if (!atual.estaAgendada()) {
            throw new OperacaoInvalidaException("Apenas consultas agendadas podem ser editadas");
        }
        if (atual.getDataHora().isBefore(LocalDateTime.now(relogio))) {
            throw new OperacaoInvalidaException("Consulta que ja passou nao pode ser editada");
        }
        validador.validar(nova);
        validarConflito(nova, nova.getId());
        consultas.atualizar(nova);
        lembretes.removerPendentesDoItem(TipoLembrete.CONSULTA, atual.getId());
    }

    // Cancela uma consulta futura do próprio paciente. Ela não é apagada: vira CANCELADA e fica no banco
    public void cancelar(Long pacienteId, Long consultaId) {
        Consulta consulta = buscarDoPaciente(pacienteId, consultaId);
        if (consulta.getDataHora().isBefore(LocalDateTime.now(relogio))) {
            throw new OperacaoInvalidaException("Consulta que ja passou nao pode ser cancelada");
        }
        consulta.cancelar();
        consultas.atualizar(consulta);
        lembretes.removerPendentesDoItem(TipoLembrete.CONSULTA, consulta.getId());
    }

    // Lista as consultas do paciente (sem as canceladas) ordenadas por data e hora
    public List<Consulta> listarDoPaciente(Long pacienteId) {
        return consultas.listarPorPaciente(pacienteId).stream()
                .sorted(Comparator.comparing(Consulta::getDataHora))
                .toList();
    }

    // Mesmo paciente não pode ter duas consultas no mesmo horário (pacientes diferentes podem)
    private void validarConflito(Consulta consulta, Long idIgnorado) {
        consultas.buscarPorPacienteEHorario(consulta.getPacienteId(), consulta.getDataHora())
                .filter(existente -> !existente.getId().equals(idIgnorado))
                .ifPresent(existente -> {
                    throw new RegistroDuplicadoException("O paciente ja tem uma consulta neste horario");
                });
    }

    private Consulta buscarDoPaciente(Long pacienteId, Long consultaId) {
        return consultas.buscarPorId(consultaId)
                .filter(c -> c.getPacienteId().equals(pacienteId))
                .orElseThrow(() -> new RegistroNaoEncontradoException("Consulta nao encontrada"));
    }

    // RN06: o dono da consulta tem que existir e ser PACIENTE
    private void exigirPaciente(Long pacienteId) {
        Usuario dono = pacienteId == null ? null : usuarios.buscarPorId(pacienteId).orElse(null);
        if (dono == null || !dono.ehPaciente()) {
            throw new DadoInvalidoException("O dono deve ser um usuario com perfil PACIENTE");
        }
    }
}
