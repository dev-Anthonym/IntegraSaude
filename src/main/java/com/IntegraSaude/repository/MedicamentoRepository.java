package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class MedicamentoRepository {

    public void inserir(int pacienteId, String nome, String dosagem, int frequenciaHoras, String primeiroHorario, String dataInicio, String dataFim) {
        String sql = "INSERT INTO medicamento (paciente_id, nome, dosagem, frequencia_horas, primeiro_horario, data_inicio, data_fim) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pacienteId);
            stmt.setString(2, nome);
            stmt.setString(3, dosagem);
            stmt.setInt(4, frequenciaHoras);
            stmt.setString(5, primeiroHorario);
            stmt.setString(6, dataInicio);
            stmt.setString(7, dataFim);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao inserir medicamento", e);
        }
    }

    public void inserir(Medicamento med) {
        int pacienteId = garantirPacientePadrao();
        inserir(pacienteId, med.getNome(), med.getDosagem(), 8, med.getHorario(), "2026-01-01", "2026-12-31");
    }

    public List<Medicamento> listarTodos() {
        List<Medicamento> lista = new ArrayList<>();
        String sql = "SELECT id, nome, dosagem, primeiro_horario FROM medicamento";
        try (Connection conn = ConexaoBanco.abrir();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new Medicamento(
                    rs.getLong("id"),
                    rs.getString("nome"),
                    rs.getString("dosagem"),
                    rs.getString("primeiro_horario")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar medicamentos", e);
        }
        return lista;
    }

    public void atualizarDosagem(long id, String novaDosagem) {
        String sql = "UPDATE medicamento SET dosagem = ? WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, novaDosagem);
            stmt.setLong(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar medicamento", e);
        }
    }

    public void deletarPorId(long id) {
        String sql = "DELETE FROM medicamento WHERE id = ?";
        try (Connection conn = ConexaoBanco.abrir();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar medicamento", e);
        }
    }

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
}