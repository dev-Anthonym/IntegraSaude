package com.IntegraSaude.repository;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoLembrete;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de lembretes. */
public interface LembreteRepository {

    // Lembrete repetido (mesmo paciente, tipo, item e horário) lança RegistroDuplicadoException (RN12)
    Lembrete salvar(Lembrete lembrete);

    // Grava o novo status do lembrete
    void atualizar(Lembrete lembrete);

    Optional<Lembrete> buscarPorId(Long id);

    // RN12: usado para não gerar lembrete duplicado
    boolean existe(Long pacienteId, TipoLembrete tipo, Long itemId, LocalDateTime horarioPrevisto);

    List<Lembrete> listarPendentesDoPaciente(Long pacienteId);

    // Pendentes de todos os pacientes cujo horário previsto é anterior ou igual ao limite
    List<Lembrete> listarPendentesAte(LocalDateTime limite);

    // Apaga os pendentes de um item (medicamento removido, consulta cancelada ou alterada)
    void removerPendentesDoItem(TipoLembrete tipo, Long itemId);
}
