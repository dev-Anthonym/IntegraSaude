package com.IntegraSaude.repository;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

// Implementação em memória, segura para uso pelo menu e pelo agendador ao mesmo tempo
public class LembreteRepositoryMemoria implements LembreteRepository {

    private final Map<Long, Lembrete> lembretes = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Lembrete salvar(Lembrete lembrete) {
        if (lembrete.getId() == null) {
            lembrete.setId(idSequence.getAndIncrement());
        }
        lembretes.put(lembrete.getId(), lembrete);
        return lembrete;
    }

    @Override
    public Optional<Lembrete> buscarPorId(Long id) {
        return Optional.ofNullable(lembretes.get(id));
    }

    @Override
    public List<Lembrete> listarPendentesDoPaciente(Long pacienteId) {
        return filtrar(l -> l.getPacienteId().equals(pacienteId)
                && l.getStatus() == StatusLembrete.PENDENTE);
    }

    @Override
    public List<Lembrete> listarPorPaciente(Long pacienteId) {
        return filtrar(l -> l.getPacienteId().equals(pacienteId));
    }

    @Override
    public List<Lembrete> listarPendentes() {
        return filtrar(l -> l.getStatus() == StatusLembrete.PENDENTE);
    }

    @Override
    public void atualizar(Lembrete lembrete) {
        lembretes.replace(lembrete.getId(), lembrete);
    }

    private List<Lembrete> filtrar(Predicate<Lembrete> criterio) {
        return lembretes.values().stream()
                .filter(criterio)
                .sorted(Comparator.comparing(Lembrete::getHorarioPrevisto))
                .toList();
    }
}
