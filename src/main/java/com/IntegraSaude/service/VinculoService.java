package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;

// Serviço responsável por gerir o vínculo entre Paciente e Responsável
public class VinculoService {

    private final UsuarioRepository usuarioRepository;

    public VinculoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Método para vincular um responsável a um paciente
    public void vincular(Long responsavelId, Long pacienteId) {
        // 1. Procura e valida o responsável
        Usuario responsavel = usuarioRepository.buscarPorId(responsavelId)
            .orElseThrow(() -> new RegistroNaoEncontradoException("Responsável não encontrado"));

        if (responsavel.getPerfil() != Perfil.RESPONSAVEL) {
            throw new DadoInvalidoException("O utilizador informado não possui o perfil de Responsável.");
        }

        // 2. Procura e valida o paciente
        Usuario paciente = usuarioRepository.buscarPorId(pacienteId)
            .orElseThrow(() -> new RegistroNaoEncontradoException("Paciente não encontrado"));

        if (paciente.getPerfil() != Perfil.PACIENTE) {
            throw new DadoInvalidoException("O utilizador informado não possui o perfil de Paciente.");
        }

        // 3. Associa o ID do responsável ao paciente e atualiza no repositório
        paciente.setResponsavelId(responsavel.getId());
        usuarioRepository.atualizar(paciente);
    }
}