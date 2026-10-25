package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    public List<Consulta> listarTodos() {
        List<Consulta> lista = new ArrayList<>();
        String sql = "SELECT * FROM consulta";
        
        try (Connection conn = ConexaoBanco.abrir();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Consulta c = new Consulta();
                c.setId(rs.getInt("id"));
                c.setPacienteId(rs.getInt("paciente_id"));
                c.setMedicoId(rs.getInt("medico_id"));
                c.setLocal(rs.getString("local"));
                c.setDataHora(rs.getString("data_hora"));
                c.setStatus(rs.getString("status"));
                lista.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar consultas", e);
        }
        return lista;
    }
}