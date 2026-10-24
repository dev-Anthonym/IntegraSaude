package com.IntegraSaude.service;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.repository.HistoricoRepository;
import com.IntegraSaude.repository.LembreteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

// Registra e consulta o histórico de eventos dos lembretes (RN09)
public class HistoricoService {

    private final HistoricoRepository historicoRepository;
    private final LembreteRepository lembreteRepository;
    private final Clock relogio;

    public HistoricoService(HistoricoRepository historicoRepository, LembreteRepository lembreteRepository,
                            Clock relogio) {
        this.historicoRepository = historicoRepository;
        this.lembreteRepository = lembreteRepository;
        this.relogio = relogio;
    }

    public EventoHistorico registrar(Lembrete lembrete, TipoEvento tipoEvento) {
        EventoHistorico evento = new EventoHistorico(null, lembrete.getId(), tipoEvento, LocalDateTime.now(relogio));
        return historicoRepository.salvar(evento);
    }

    public List<EventoHistorico> listarDoPaciente(Long pacienteId) {
        return lembreteRepository.listarPorPaciente(pacienteId).stream()
                .flatMap(lembrete -> historicoRepository.listarPorLembrete(lembrete.getId()).stream())
                .sorted(Comparator.comparing(EventoHistorico::getDataHora))
                .toList();
    }

    // Período fechado: inclui eventos exatamente no início e no fim
    public List<EventoHistorico> listarDoPacienteNoPeriodo(Long pacienteId, LocalDateTime inicio, LocalDateTime fim) {
        return listarDoPaciente(pacienteId).stream()
                .filter(evento -> !evento.getDataHora().isBefore(inicio) && !evento.getDataHora().isAfter(fim))
                .toList();
    }
}
