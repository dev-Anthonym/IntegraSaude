package com.IntegraSaude.repository;

import com.IntegraSaude.model.EventoHistorico;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

// Implementação em memória do histórico, segura para uso pelo menu e pelo agendador
public class HistoricoRepositoryMemoria implements HistoricoRepository {

    private final List<EventoHistorico> eventos = new CopyOnWriteArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public EventoHistorico salvar(EventoHistorico evento) {
        if (evento.getId() == null) {
            evento.setId(idSequence.getAndIncrement());
        }
        eventos.add(evento);
        return evento;
    }

    @Override
    public List<EventoHistorico> listarPorLembrete(Long lembreteId) {
        return eventos.stream()
                .filter(evento -> evento.getLembreteId().equals(lembreteId))
                .toList();
    }
}
