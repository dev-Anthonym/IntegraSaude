package com.IntegraSaude.service;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.repository.HistoricoRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** Registra e consulta o histórico de eventos (RN13). */
public class HistoricoService {

    private final HistoricoRepository repositorio;
    private final Clock relogio;

    public HistoricoService(HistoricoRepository repositorio, Clock relogio) {
        this.repositorio = repositorio;
        this.relogio = relogio;
    }

    // Grava um evento com a data/hora atual do relógio injetado
    public EventoHistorico registrar(Long pacienteId, Long lembreteId, TipoEvento tipo, String descricao) {
        return repositorio.salvar(new EventoHistorico(pacienteId, lembreteId, tipo,
                LocalDateTime.now(relogio), descricao));
    }

    // Histórico completo do paciente, ordenado por data
    public List<EventoHistorico> listarDoPaciente(Long pacienteId) {
        return repositorio.listarPorPaciente(pacienteId);
    }

    // Histórico do paciente entre duas datas/horas (inclusive), ordenado por data
    public List<EventoHistorico> listarPorPeriodo(Long pacienteId, LocalDateTime de, LocalDateTime ate) {
        return repositorio.listarPorPeriodo(pacienteId, de, ate);
    }

    // Verdadeiro se o lembrete já tem um evento daquele tipo (usado para alertar só uma vez)
    public boolean jaRegistrado(Long lembreteId, TipoEvento tipo) {
        return repositorio.existeEvento(lembreteId, tipo);
    }
}
