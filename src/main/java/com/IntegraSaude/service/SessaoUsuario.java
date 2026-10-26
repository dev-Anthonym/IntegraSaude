package com.IntegraSaude.service;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.model.Usuario;
import java.util.Optional;

/** Guarda em memória quem está logado. */
public class SessaoUsuario {

    private Usuario usuarioLogado;

    public synchronized void login(Usuario usuario) {
        this.usuarioLogado = usuario;
    }

    public synchronized void logout() {
        this.usuarioLogado = null;
    }

    public synchronized boolean estaLogado() {
        return usuarioLogado != null;
    }

    public synchronized Optional<Usuario> usuario() {
        return Optional.ofNullable(usuarioLogado);
    }

    // Devolve o usuário logado ou falha se ninguém estiver logado
    public synchronized Usuario exigirLogado() {
        if (usuarioLogado == null) {
            throw new OperacaoInvalidaException("E necessario fazer login");
        }
        return usuarioLogado;
    }
}
