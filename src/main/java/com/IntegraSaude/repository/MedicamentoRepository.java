package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.*;
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
        inserir(med.getPacienteId(), med.getNome(), med.getDosagem(), med.getFrequenciaHoras(), med.getPrimeiroHorario(), med.getDataInicio(), med.getDataFim());
    }

    public List<Medicamento> listarTodos() {
        List<Medicamento> lista = new ArrayList<>();
        String sql = "SELECT * FROM medicamento";
        
        try (Connection conn = ConexaoBanco.abrir();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Medicamento med = new Medicamento();
                med.setId(rs.getInt("id"));
                med.setPacienteId(rs.getInt("paciente_id"));
                med.setNome(rs.getString("nome"));
                med.setDosagem(rs.getString("dosagem"));
                med.setFrequenciaHoras(rs.getInt("frequencia_horas"));
                med.setPrimeiroHorario(rs.getString("primeiro_horario"));
                med.setDataInicio(rs.getString("data_inicio"));
                med.setDataFim(rs.getString("data_fim"));
                lista.add(med);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar medicamentos", e);
        }
        return lista;
    }
}