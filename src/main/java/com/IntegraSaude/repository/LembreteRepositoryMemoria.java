package com.IntegraSaude.repository;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoLembrete;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Lembretes em memória. Simula o UNIQUE(paciente, tipo, item, horário) da RN12. */
public class LembreteRepositoryMemoria implements LembreteRepository {

    private final TabelaMemoria<Lembrete> tabela = new TabelaMemoria<>();

    @Override
    public synchronized Lembrete salvar(Lembrete lembrete) {
        if (existe(lembrete.getPacienteId(), lembrete.getTipo(), lembrete.getItemId(),
                lembrete.getHorarioPrevisto())) {
            throw new RegistroDuplicadoException("Lembrete duplicado para o mesmo item e horario");
        }
        return tabela.inserir(lembrete);
    }

    @Override
    public void atualizar(Lembrete lembrete) {
        tabela.substituir(lembrete);
    }

    @Override
    public Optional<Lembrete> buscarPorId(Long id) {
        return tabela.buscar(id);
    }

    @Override
    public boolean existe(Long pacienteId, TipoLembrete tipo, Long itemId, LocalDateTime horarioPrevisto) {
        return tabela.existe(l -> l.getPacienteId().equals(pacienteId) && l.getTipo() == tipo
                && l.getItemId().equals(itemId) && l.getHorarioPrevisto().equals(horarioPrevisto));
    }

    @Override
    public List<Lembrete> listarPendentesDoPaciente(Long pacienteId) {
        return tabela.filtrar(l -> l.getPacienteId().equals(pacienteId) && l.estaPendente());
    }

    @Override
    public List<Lembrete> listarPendentesAte(LocalDateTime limite) {
        return tabela.filtrar(l -> l.estaPendente() && !l.getHorarioPrevisto().isAfter(limite));
    }

    @Override
    public void removerPendentesDoItem(TipoLembrete tipo, Long itemId) {
        tabela.removerSe(l -> l.estaPendente() && l.getTipo() == tipo && l.getItemId().equals(itemId));
    }
}
