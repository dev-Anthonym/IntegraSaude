package com.IntegraSaude.repository;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.TipoEvento;
import java.time.LocalDateTime;
import java.util.List;

/** Acesso a dados do histórico (RN13). Só insere e consulta: histórico não se altera. */
public interface HistoricoRepository {

    EventoHistorico salvar(EventoHistorico evento);

    // Todos os eventos do paciente, ordenados por data/hora
    List<EventoHistorico> listarPorPaciente(Long pacienteId);

    // Eventos entre as duas datas (inclusive), ordenados por data/hora
    List<EventoHistorico> listarPorPeriodo(Long pacienteId, LocalDateTime de, LocalDateTime ate);

    // Verdadeiro se já existe evento daquele tipo para o lembrete (garante o alerta uma única vez)
    boolean existeEvento(Long lembreteId, TipoEvento tipo);
}
