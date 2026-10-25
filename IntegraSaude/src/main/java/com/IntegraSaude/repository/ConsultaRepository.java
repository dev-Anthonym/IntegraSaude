package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A classe ConsultaRepository gerencia a comunicação com o banco de dados
 * para realizar operações de cadastro e consulta de agendamentos médicos.
 */
public class ConsultaRepository {

    /**
     * Insere uma nova consulta no banco de dados SQLite.
     * @param consulta Objeto contendo os dados a serem salvos
     */
    public void inserir(Consulta consulta) {
        String sql = "INSERT INTO consultas (medico, especialidade, data_hora) VALUES (?, ?, ?)";

        try (Connection conn = Conexao.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, consulta.getMedico());
            pstmt.setString(2, consulta.getEspecialidade());
            pstmt.setString(3, consulta.getDataHora() != null ? consulta.getDataHora().toString() : null);
            
            pstmt.executeUpdate();
            System.out.println("-> Consulta cadastrada com sucesso no banco de dados!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir consulta: " + e.getMessage());
        }
    }

    /**
     * Lista todas as consultas cadastradas no banco de dados.
     * @return Uma lista com todas as consultas encontradas
     */
    public List<Consulta> listarTodos() {
        List<Consulta> consultas = new ArrayList<>();
        String sql = "SELECT id, medico, especialidade, data_hora FROM consultas";

        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Consulta cons = new Consulta();
                cons.setId(rs.getLong("id"));
                cons.setMedico(rs.getString("medico"));
                cons.setEspecialidade(rs.getString("especialidade"));
                String dataHoraStr = rs.getString("data_hora");
                    if (dataHoraStr != null) {
                        cons.setDataHora(LocalDateTime.parse(dataHoraStr));
                    }

                consultas.add(cons);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar consultas: " + e.getMessage());
        }

        return consultas;
    }
}