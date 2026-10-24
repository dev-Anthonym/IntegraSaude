package com.IntegraSaude.service;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

// Lógica executada a cada ciclo do agendador, sem depender de threads (testável com qualquer horário)
public class VerificadorLembretes {

    private static final Logger LOGGER = Logger.getLogger(VerificadorLembretes.class.getName());

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
                .ifPresent(paciente -> enviarAviso(paciente, lembrete));
    }

    // Uma falha ao avisar um paciente não impede os avisos dos demais
    private void enviarAviso(Usuario paciente, Lembrete lembrete) {
        try {
            notificador.notificar(paciente, montarAviso(lembrete));
        } catch (RuntimeException e) {
            LOGGER.warning("Falha ao avisar o paciente " + paciente.getId() + ": " + e.getMessage());
        }
    }

    private String montarAviso(Lembrete lembrete) {
        String horario = lembrete.getHorarioPrevisto().format(DateTimeFormatter.ofPattern("HH:mm"));
        String acao = lembrete.getTipo() == TipoLembrete.MEDICAMENTO ? "hora de tomar" : "não esqueça da consulta";
        return "LEMBRETE #" + lembrete.getId() + " (" + horario + "): " + acao + " "
                + lembrete.getDescricao() + ". Confirme pelo menu.";
    }
}
