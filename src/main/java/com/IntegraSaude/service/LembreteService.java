package com.IntegraSaude.service;

import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.exception.TransicaoInvalidaException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.repository.LembreteRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Serviço que confirma lembretes e marca como perdidos os que passaram do tempo limite (RN03 a RN05)
public class LembreteService {

    private final LembreteRepository lembreteRepository;
    private final HistoricoService historicoService;
    private final Clock relogio;
    private final Duration tempoLimite;

    public LembreteService(LembreteRepository lembreteRepository, HistoricoService historicoService,
                           Clock relogio, Duration tempoLimite) {
        this.lembreteRepository = lembreteRepository;
        this.historicoService = historicoService;
        this.relogio = relogio;
        this.tempoLimite = tempoLimite;
    }

    // Confirma o lembrete ("tomei" ou "compareci") se ainda estiver dentro do prazo
    public synchronized Lembrete confirmar(Long lembreteId) {
        Lembrete lembrete = buscarPorId(lembreteId);
        if (lembrete.estaVencidoEm(LocalDateTime.now(relogio), tempoLimite)) {
            throw new TransicaoInvalidaException("O prazo para confirmar este lembrete já expirou.");
        }
        lembrete.confirmar();
        lembreteRepository.atualizar(lembrete);
        historicoService.registrar(lembrete, TipoEvento.CONFIRMADO);
        return lembrete;
    }

    // RN04: marca como perdidos os lembretes pendentes vencidos há mais que o tempo limite
    public synchronized List<Lembrete> verificarPerdidos(LocalDateTime agora) {
        List<Lembrete> perdidos = new ArrayList<>();
        for (Lembrete lembrete : lembreteRepository.listarPendentes()) {
            if (lembrete.estaVencidoEm(agora, tempoLimite)) {
                lembrete.marcarComoPerdido();
                lembreteRepository.atualizar(lembrete);
                historicoService.registrar(lembrete, TipoEvento.PERDIDO);
                perdidos.add(lembrete);
            }
        }
        return perdidos;
    }

    public List<Lembrete> listarPendentes(Long pacienteId) {
        return lembreteRepository.listarPendentesDoPaciente(pacienteId);
    }

    // Lembretes pendentes cujo horário previsto já chegou
    public List<Lembrete> listarPendentesAte(LocalDateTime agora) {
        return lembreteRepository.listarPendentes().stream()
                .filter(lembrete -> !lembrete.getHorarioPrevisto().isAfter(agora))
                .toList();
    }

    public Lembrete buscarPorId(Long lembreteId) {
        return lembreteRepository.buscarPorId(lembreteId)
                .orElseThrow(() -> new RegistroNaoEncontradoException(
                        "Lembrete não encontrado: " + lembreteId));
    }
}
