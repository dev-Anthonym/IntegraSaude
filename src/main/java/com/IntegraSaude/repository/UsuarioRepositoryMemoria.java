package com.IntegraSaude.repository;

import com.IntegraSaude.model.Usuario;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

// Implementação do repositório em memória utilizando List para testes unitários sem banco
public class UsuarioRepositoryMemoria implements UsuarioRepository {
    
    private final List<Usuario> usuarios = new ArrayList<>();
    private final AtomicLong idSequence = new AtomicLong(1); // Gerador de IDs automáticos

    @Override
    public Usuario salvar(Usuario usuario) {
        if (usuario.getId() == null) {
            usuario.setId(idSequence.getAndIncrement());
        }
        usuarios.add(usuario);
        return usuario;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarios.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return usuarios.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    @Override
    public void atualizar(Usuario usuario) {
    if (usuario != null && usuario.getId() != null) {
        for (int i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).getId().equals(usuario.getId())) {
                usuarios.set(i, usuario);
                return;
                }
            }
        }
    }
}