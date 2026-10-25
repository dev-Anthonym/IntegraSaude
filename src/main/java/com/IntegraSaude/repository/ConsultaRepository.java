package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.*;

public class ConsultaRepository {

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

    public void inserir(Consulta consulta) {
        inserir(consulta.getPacienteId(), consulta.getMedicoId(), consulta.getLocal(), consulta.getDataHora(), consulta.getStatus());
    }
}