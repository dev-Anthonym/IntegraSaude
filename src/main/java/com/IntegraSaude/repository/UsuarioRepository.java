package com.IntegraSaude.repository;

import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório JDBC responsável pelas operações de persistência (CRUD) da tabela 'usuario'.
 * Utiliza PreparedStatement em todas as consultas para prevenção contra SQL Injection.
 */
public class UsuarioRepository {

    /**
     * Insere um novo usuário (PACIENTE ou RESPONSAVEL) no banco de dados.
     */
    public void inserir(String nome, String email, String senhaHash, String perfil, String dataCadastro) {
        String sql = "INSERT INTO usuario (nome, email, senha_hash, perfil, data_cadastro) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nome);
            stmt.setString(2, email);
            stmt.setString(3, senhaHash);
            stmt.setString(4, perfil);
            stmt.setString(5, dataCadastro);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir usuario", e);
        }
    }

    /**
     * Busca o nome de um usuário pelo seu identificador único (ID).
     */
    public Optional<String> buscarNomePorId(int id) {
        String sql = "SELECT nome FROM usuario WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("nome"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuario por ID", e);
        }
        return Optional.empty();
    }

    /**
     * Lista todos os e-mails de usuários cadastrados ordenados pelo ID.
     */
    public List<String> listarEmails() {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT email FROM usuario ORDER BY id";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                emails.add(rs.getString("email"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar usuarios", e);
        }
        return emails;
    }

    /**
     * Atualiza o nome de um usuário existente a partir do seu ID.
     */
    public void atualizarNome(int id, String novoNome) {
        String sql = "UPDATE usuario SET nome = ? WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar usuario", e);
        }
    }

    /**
     * Remove um usuário do banco de dados pelo seu ID.
     */
    public void deletarPorId(int id) {
        String sql = "DELETE FROM usuario WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar usuario", e);
        }
    }
}