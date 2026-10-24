package com.IntegraSaude.service;

import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;

// Adaptador que lê o responsável vinculado ao paciente pelo cadastro de usuários
public class ResponsaveisPorVinculo implements BuscaResponsaveis {

    private final UsuarioRepository usuarioRepository;

    public ResponsaveisPorVinculo(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public List<Usuario> listarResponsaveisDoPaciente(Long pacienteId) {
        return usuarioRepository.buscarPorId(pacienteId)
                .map(Usuario::getResponsavelId)
                .flatMap(this::buscarResponsavel)
                .map(List::of)
                .orElse(List.of());
    }

    private Optional<Usuario> buscarResponsavel(Long responsavelId) {
        return usuarioRepository.buscarPorId(responsavelId);
    }
}
