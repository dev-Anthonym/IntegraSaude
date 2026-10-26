package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.model.Vinculo;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.VinculoRepository;
import java.util.List;

/** Regras do vínculo paciente-responsável (RN03). */
public class VinculoService {

    private final VinculoRepository vinculos;
    private final UsuarioRepository usuarios;

    public VinculoService(VinculoRepository vinculos, UsuarioRepository usuarios) {
        this.vinculos = vinculos;
        this.usuarios = usuarios;
    }

    // Cria o vínculo validando: não é consigo mesmo, perfis corretos e sem duplicidade
    public Vinculo criar(Long pacienteId, Long responsavelId) {
        if (pacienteId.equals(responsavelId)) {
            throw new DadoInvalidoException("Um usuario nao pode se vincular a si mesmo");
        }
        Usuario paciente = buscar(pacienteId);
        Usuario responsavel = buscar(responsavelId);
        if (!paciente.ehPaciente()) {
            throw new DadoInvalidoException("O usuario informado como paciente nao tem perfil PACIENTE");
        }
        if (!responsavel.ehResponsavel()) {
            throw new DadoInvalidoException("O usuario informado como responsavel nao tem perfil RESPONSAVEL");
        }
        if (vinculos.buscarPorPar(pacienteId, responsavelId).isPresent()) {
            throw new RegistroDuplicadoException("Este vinculo ja existe");
        }
        return vinculos.salvar(new Vinculo(pacienteId, responsavelId));
    }

    // Remove o vínculo do par informado
    public void remover(Long pacienteId, Long responsavelId) {
        Vinculo vinculo = vinculos.buscarPorPar(pacienteId, responsavelId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Vinculo nao encontrado"));
        vinculos.remover(vinculo.getId());
    }

    // Responsáveis de um paciente (quem recebe o alerta, RN11)
    public List<Usuario> listarResponsaveisDoPaciente(Long pacienteId) {
        return vinculos.listarPorPaciente(pacienteId).stream()
                .map(v -> buscar(v.getResponsavelId()))
                .toList();
    }

    // Pacientes acompanhados por um responsável
    public List<Usuario> listarPacientesDoResponsavel(Long responsavelId) {
        return vinculos.listarPorResponsavel(responsavelId).stream()
                .map(v -> buscar(v.getPacienteId()))
                .toList();
    }

    private Usuario buscar(Long id) {
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Usuario nao encontrado"));
    }
}
