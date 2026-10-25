package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório JDBC responsável pelas operações de persistência (CRUD) da tabela 'consulta'.
 * Gerencia automaticamente o relacionamento na 3FN com as tabelas 'medico' e 'especialidade'.
 */
public class ConsultaRepository {

    /**
     * Insere uma consulta diretamente informando os IDs de paciente e médico.
     */
    public void inserir(int pacienteId, int medicoId, String local, String dataHora, String status) {
        String sql = "INSERT INTO consulta (paciente_id, medico_id, local, data_hora, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            stmt.setInt(2, medicoId);
            stmt.setString(3, local);
            stmt.setString(4, dataHora);
            stmt.setString(5, status);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir consulta", e);
        }
    }

    /**
     * Sobrecarga que recebe o objeto Consulta e garante o cadastro normalizado
     * da especialidade e do médico antes de gravar o agendamento.
     */
    public void inserir(Consulta consulta) {
        int pacienteId = garantirPacientePadrao();
        int especialidadeId = garantirEspecialidade(consulta.getEspecialidade());
        int medicoId = garantirMedico(consulta.getMedico(), especialidadeId);
        inserir(pacienteId, medicoId, "Consultorio Principal", consulta.getDataHora(), "AGENDADA");
    }

    /**
     * Lista todas as consultas realizando JOIN com 'medico' e 'especialidade'.
     */
    public List<Consulta> listarTodos() {
        List<Consulta> lista = new ArrayList<>();
        String sql = "SELECT c.id, m.nome AS medico, e.nome AS especialidade, c.data_hora " +
                     "FROM consulta c " +
                     "JOIN medico m ON m.id = c.medico_id " +
                     "JOIN especialidade e ON e.id = m.especialidade_id";
        try (Connection conn = ConexaoBanco.abrir();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Consulta(
                    rs.getLong("id"),
                    rs.getString("medico"),
                    rs.getString("especialidade"),
                    rs.getString("data_hora")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar consultas", e);
        }
        return lista;
    }

    /**
     * Atualiza o status de uma consulta (AGENDADA, REALIZADA ou CANCELADA).
     */
    public void atualizarStatus(long id, String novoStatus) {
        String sql = "UPDATE consulta SET status = ? WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novoStatus);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status da consulta", e);
        }
    }

    /**
     * Remove uma consulta do banco de dados pelo seu ID.
     */
    public void deletarPorId(long id) {
        String sql = "DELETE FROM consulta WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar consulta", e);
        }
    }

    /**
     * Busca ou cria o paciente padrão para vincular à consulta.
     */
    private int garantirPacientePadrao() {
        String sqlBusca = "SELECT id FROM usuario WHERE email = 'paciente.padrao@integrasaude.com'";
        String sqlInsere = "INSERT INTO usuario (nome, email, senha_hash, perfil, data_cadastro) VALUES ('Paciente Padrao', 'paciente.padrao@integrasaude.com', 'HASH', 'PACIENTE', '2026-01-01T00:00:00')";
        try (Connection conn = ConexaoBanco.abrir();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sqlBusca)) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            stmt.executeUpdate(sqlInsere);
            try (ResultSet rs = stmt.executeQuery(sqlBusca)) {
                rs.next();
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao garantir paciente padrao", e);
        }
    }

    /**
     * Busca o ID de uma especialidade pelo nome ou a insere caso ainda não exista (3FN).
     */
    private int garantirEspecialidade(String nomeEspecialidade) {
        String sqlBusca = "SELECT id FROM especialidade WHERE nome = ?";
        String sqlInsere = "INSERT INTO especialidade (nome) VALUES (?)";
        try (Connection conn = ConexaoBanco.abrir()) {
            try (PreparedStatement stmt = conn.prepareStatement(sqlBusca)) {
                stmt.setString(1, nomeEspecialidade);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsere, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, nomeEspecialidade);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao garantir especialidade", e);
        }
    }

    /**
     * Busca o ID de um médico pelo nome e especialidade ou o insere caso não exista (3FN).
     */
    private int garantirMedico(String nomeMedico, int especialidadeId) {
        String sqlBusca = "SELECT id FROM medico WHERE nome = ? AND especialidade_id = ?";
        String sqlInsere = "INSERT INTO medico (nome, especialidade_id) VALUES (?, ?)";
        try (Connection conn = ConexaoBanco.abrir()) {
            try (PreparedStatement stmt = conn.prepareStatement(sqlBusca)) {
                stmt.setString(1, nomeMedico);
                stmt.setInt(2, especialidadeId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("id");
                    }
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsere, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, nomeMedico);
                stmt.setInt(2, especialidadeId);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao garantir medico", e);
        }
    }
}