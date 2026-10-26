package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.MedicamentoRepository;
import com.IntegraSaude.repository.UsuarioRepository;
import java.util.Comparator;
import java.util.List;

/** Cadastro e manutenção de medicamentos (RN05 e RN06). */
public class MedicamentoService {

    private final MedicamentoRepository medicamentos;
    private final UsuarioRepository usuarios;
    private final LembreteRepository lembretes;
    private final ValidadorMedicamento validador;

    public MedicamentoService(MedicamentoRepository medicamentos, UsuarioRepository usuarios,
                              LembreteRepository lembretes, ValidadorMedicamento validador) {
        this.medicamentos = medicamentos;
        this.usuarios = usuarios;
        this.lembretes = lembretes;
        this.validador = validador;
    }

    // Cadastra: o dono precisa ser PACIENTE (RN06) e os dados precisam ser válidos (RN05)
    public Medicamento cadastrar(Medicamento medicamento) {
        exigirPaciente(medicamento.getPacienteId());
        validador.validar(medicamento);
        return medicamentos.salvar(medicamento);
    }

    // Atualiza um medicamento do próprio paciente; lembretes pendentes antigos são descartados
    public void atualizar(Medicamento novo) {
        Medicamento atual = buscarDoPaciente(novo.getPacienteId(), novo.getId());
        exigirPaciente(novo.getPacienteId());
        validador.validar(novo);
        medicamentos.atualizar(novo);
        lembretes.removerPendentesDoItem(TipoLembrete.MEDICAMENTO, atual.getId());
    }

    // Remove um medicamento do próprio paciente e os lembretes pendentes dele
    public void remover(Long pacienteId, Long medicamentoId) {
        Medicamento medicamento = buscarDoPaciente(pacienteId, medicamentoId);
        lembretes.removerPendentesDoItem(TipoLembrete.MEDICAMENTO, medicamento.getId());
        medicamentos.remover(medicamento.getId());
    }

    // Lista os medicamentos do paciente por nome
    public List<Medicamento> listarDoPaciente(Long pacienteId) {
        return medicamentos.listarPorPaciente(pacienteId).stream()
                .sorted(Comparator.comparing(m -> m.getNome().toLowerCase()))
                .toList();
    }

    // Busca garantindo que o medicamento pertence ao paciente informado
    private Medicamento buscarDoPaciente(Long pacienteId, Long medicamentoId) {
        return medicamentos.buscarPorId(medicamentoId)
                .filter(m -> m.getPacienteId().equals(pacienteId))
                .orElseThrow(() -> new RegistroNaoEncontradoException("Medicamento nao encontrado"));
    }

    // RN06: o dono do medicamento tem que existir e ser PACIENTE
    private void exigirPaciente(Long pacienteId) {
        Usuario dono = pacienteId == null ? null : usuarios.buscarPorId(pacienteId).orElse(null);
        if (dono == null || !dono.ehPaciente()) {
            throw new DadoInvalidoException("O dono deve ser um usuario com perfil PACIENTE");
        }
    }
}
