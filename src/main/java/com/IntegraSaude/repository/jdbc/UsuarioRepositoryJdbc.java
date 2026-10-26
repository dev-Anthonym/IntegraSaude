package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Usuários no SQLite. */
public class UsuarioRepositoryJdbc extends RepositorioJdbc implements UsuarioRepository {

    private static final String COLUNAS = "SELECT id, nome, email, senha_hash, perfil FROM usuario ";

    public UsuarioRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        // data_cadastro é obrigatória no banco: o próprio SQLite grava a data/hora atual em ISO-8601
        long id = inserir("INSERT INTO usuario (nome, email, senha_hash, perfil, data_cadastro) "
                        + "VALUES (?, ?, ?, ?, strftime('%Y-%m-%dT%H:%M:%S', 'now', 'localtime'))",
                usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash(), usuario.getPerfil());
        return comId(usuario, id);
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return consultarUm(COLUNAS + "WHERE id = ?", this::mapear, id);
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        return consultarUm(COLUNAS + "WHERE email = ?", this::mapear, email);
    }

    @Override
    public List<Usuario> listarPorPerfil(Perfil perfil) {
        return consultar(COLUNAS + "WHERE perfil = ? ORDER BY nome", this::mapear, perfil);
    }

    // Linha do banco -> objeto Usuario
    private Usuario mapear(ResultSet linha) throws SQLException {
        return comId(new Usuario(linha.getString("nome"), linha.getString("email"),
                linha.getString("senha_hash"), Perfil.valueOf(linha.getString("perfil"))), linha.getLong("id"));
    }
}
