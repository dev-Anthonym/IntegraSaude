package com.IntegraSaude.repository;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import java.util.List;
import java.util.Optional;

/** Usuários em memória. Simula a restrição UNIQUE do e-mail. */
public class UsuarioRepositoryMemoria implements UsuarioRepository {

    private final TabelaMemoria<Usuario> tabela = new TabelaMemoria<>();

    @Override
    public synchronized Usuario salvar(Usuario usuario) {
        // Simula o UNIQUE(email) do banco
        if (tabela.existe(u -> u.getEmail().equalsIgnoreCase(usuario.getEmail()))) {
            throw new RegistroDuplicadoException("E-mail ja cadastrado");
        }
        return tabela.inserir(usuario);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return tabela.buscar(id);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return tabela.filtrar(u -> u.getEmail().equalsIgnoreCase(email)).stream().findFirst();
    }

    @Override
    public List<Usuario> listarPorPerfil(Perfil perfil) {
        return tabela.filtrar(u -> u.getPerfil() == perfil);
    }
}
