package com.IntegraSaude.repository;

import com.IntegraSaude.model.EventoHistorico;
import java.util.List;

// Contrato de persistência do histórico de eventos dos lembretes
public interface HistoricoRepository {
    EventoHistorico salvar(EventoHistorico evento);
    List<EventoHistorico> listarPorLembrete(Long lembreteId);
}
