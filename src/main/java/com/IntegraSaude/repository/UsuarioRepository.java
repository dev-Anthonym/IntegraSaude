package com.IntegraSaude.repository;

import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UsuarioRepository {

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
            throw new RuntimeException("Erro ao inserir usuário", e);
        }
    }

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
            throw new RuntimeException("Erro ao buscar usuário por ID", e);
        }
        return Optional.empty();
    }
}