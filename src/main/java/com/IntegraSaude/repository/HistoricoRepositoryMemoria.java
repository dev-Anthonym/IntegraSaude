package com.IntegraSaude.repository;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.TipoEvento;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** Histórico em memória. */
public class HistoricoRepositoryMemoria implements HistoricoRepository {

    private final TabelaMemoria<EventoHistorico> tabela = new TabelaMemoria<>();

    @Override
    public EventoHistorico salvar(EventoHistorico evento) {
        return tabela.inserir(evento);
    }

    @Override
    public List<EventoHistorico> listarPorPaciente(Long pacienteId) {
        return ordenado(tabela.filtrar(e -> e.getPacienteId().equals(pacienteId)));
    }

    @Override
    public List<EventoHistorico> listarPorPeriodo(Long pacienteId, LocalDateTime de, LocalDateTime ate) {
        return ordenado(tabela.filtrar(e -> e.getPacienteId().equals(pacienteId)
                && !e.getDataHora().isBefore(de) && !e.getDataHora().isAfter(ate)));
    }

    @Override
    public boolean existeEvento(Long lembreteId, TipoEvento tipo) {
        return tabela.existe(e -> lembreteId.equals(e.getLembreteId()) && e.getTipo() == tipo);
    }

    // Ordena por data/hora; o id desempata eventos no mesmo instante, mantendo a ordem de criação
    private List<EventoHistorico> ordenado(List<EventoHistorico> eventos) {
        return eventos.stream()
                .sorted(Comparator.comparing(EventoHistorico::getDataHora)
                        .thenComparing(EventoHistorico::getId))
                .toList();
    }
}
