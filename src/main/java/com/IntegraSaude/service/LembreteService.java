package com.IntegraSaude.service;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.DescricaoLembrete;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.repository.LembreteRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Confirmação de doses e detecção de lembretes perdidos (RN09 e RN10). */
public class LembreteService {

    private final LembreteRepository repositorio;
    private final HistoricoService historico;
    private final Clock relogio;
    private final int tempoLimiteMinutos;

    public LembreteService(LembreteRepository repositorio, HistoricoService historico,
                           Clock relogio, ConfiguracaoLembretes configuracao) {
        this.repositorio = repositorio;
        this.historico = historico;
        this.relogio = relogio;
        this.tempoLimiteMinutos = configuracao.tempoLimiteMinutos();
    }

    // Confirma um lembrete do próprio paciente. Depois do prazo a confirmação é rejeitada (RN10)
    public Lembrete confirmar(Long pacienteId, Long lembreteId) {
        Lembrete lembrete = repositorio.buscarPorId(lembreteId)
                .filter(l -> l.getPacienteId().equals(pacienteId))
                .orElseThrow(() -> new RegistroNaoEncontradoException("Lembrete nao encontrado"));
        if (lembrete.estaPendente() && prazoExpirado(lembrete, LocalDateTime.now(relogio))) {
            throw new OperacaoInvalidaException("O prazo para confirmar este lembrete ja expirou");
        }
        lembrete.confirmar();
        repositorio.atualizar(lembrete);
        historico.registrar(pacienteId, lembrete.getId(), TipoEvento.CONFIRMADO,
                DescricaoLembrete.evento(TipoEvento.CONFIRMADO, lembrete.getDescricao()));
        return lembrete;
    }

    // Pendentes do paciente ordenados pelo horário previsto
    public List<Lembrete> listarPendentes(Long pacienteId) {
        return repositorio.listarPendentesDoPaciente(pacienteId).stream()
                .sorted(Comparator.comparing(Lembrete::getHorarioPrevisto))
                .toList();
    }

    // Pendentes (de todos os pacientes) cujo horário já chegou: usado para avisar o paciente
    public List<Lembrete> listarDevidos(LocalDateTime agora) {
        return repositorio.listarPendentesAte(agora);
    }

    // Marca como PERDIDO os pendentes vencidos há mais de X minutos e devolve quais foram marcados
    public List<Lembrete> verificarPerdidos(LocalDateTime agora) {
        List<Lembrete> perdidos = new ArrayList<>();
        for (Lembrete lembrete : repositorio.listarPendentesAte(agora)) {
            if (prazoExpirado(lembrete, agora)) {
                lembrete.marcarComoPerdido();
                repositorio.atualizar(lembrete);
                historico.registrar(lembrete.getPacienteId(), lembrete.getId(), TipoEvento.PERDIDO,
                        DescricaoLembrete.evento(TipoEvento.PERDIDO, lembrete.getDescricao()));
                perdidos.add(lembrete);
            }
        }
        return perdidos;
    }

    // Venceu quando "agora" passa de horário previsto + tempo limite
    private boolean prazoExpirado(Lembrete lembrete, LocalDateTime agora) {
        return agora.isAfter(lembrete.getHorarioPrevisto().plusMinutes(tempoLimiteMinutos));
    }
}
