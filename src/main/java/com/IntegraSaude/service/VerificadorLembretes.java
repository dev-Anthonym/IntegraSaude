package com.IntegraSaude.service;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Lógica executada a cada ciclo do agendador, sem depender de threads (testável com qualquer horário)
public class VerificadorLembretes {

    private final LembreteService lembreteService;
    private final AlertaService alertaService;
    private final UsuarioRepository usuarioRepository;
    private final Notificador notificador;
    private final Set<Long> lembretesAvisados = ConcurrentHashMap.newKeySet();

    public VerificadorLembretes(LembreteService lembreteService, AlertaService alertaService,
                                UsuarioRepository usuarioRepository, Notificador notificador) {
        this.lembreteService = lembreteService;
        this.alertaService = alertaService;
        this.usuarioRepository = usuarioRepository;
        this.notificador = notificador;
    }

    public void verificar(LocalDateTime agora) {
        lembreteService.verificarPerdidos(agora).forEach(alertaService::alertar);
        lembreteService.listarPendentesAte(agora).forEach(this::avisarPaciente);
    }

    // Avisa o paciente uma única vez quando chega o horário do lembrete
    private void avisarPaciente(Lembrete lembrete) {
        if (!lembretesAvisados.add(lembrete.getId())) {
            return;
        }
        usuarioRepository.buscarPorId(lembrete.getPacienteId())
                .ifPresent(paciente -> notificador.notificar(paciente, montarAviso(lembrete)));
    }

    private String montarAviso(Lembrete lembrete) {
        String horario = lembrete.getHorarioPrevisto().format(DateTimeFormatter.ofPattern("HH:mm"));
        String acao = lembrete.getTipo() == TipoLembrete.MEDICAMENTO ? "hora de tomar" : "não esqueça da consulta";
        return "LEMBRETE #" + lembrete.getId() + " (" + horario + "): " + acao + " "
                + lembrete.getDescricao() + ". Confirme pelo menu.";
    }
}
