package com.IntegraSaude.repository;

import com.IntegraSaude.model.Usuario;
import java.util.Optional;

// Interface que define o contrato de persistência de usuários
public interface UsuarioRepository {
    Usuario salvar(Usuario usuario);
    Optional<Usuario> buscarPorEmail(String email);
    Optional<Usuario> buscarPorId(Long id);
}