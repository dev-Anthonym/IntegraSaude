package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medicamento;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A classe MedicamentoRepository gerencia a comunicação com o banco de dados
 * para realizar operações de cadastro e consulta de medicamentos.
 */
public class MedicamentoRepository {

    /**
     * Insere um novo medicamento no banco de dados SQLite.
     * @param medicamento Objeto contendo os dados a serem salvos
     */
    public void inserir(Medicamento medicamento) {
        String sql = "INSERT INTO medicamentos (nome, dosagem, horario) VALUES (?, ?, ?)";

        try (Connection conn = Conexao.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, medicamento.getNome());
            pstmt.setString(2, medicamento.getDosagem());
            pstmt.setString(3, medicamento.getHorario());
            
            pstmt.executeUpdate();
            System.out.println("-> Medicamento cadastrado com sucesso no banco de dados!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir medicamento: " + e.getMessage());
        }
    }

    /**
     * Lista todos os medicamentos cadastrados no banco de dados.
     * @return Uma lista com todos os medicamentos encontrados
     */
    public List<Medicamento> listarTodos() {
        List<Medicamento> medicamentos = new ArrayList<>();
        String sql = "SELECT id, nome, dosagem, horario FROM medicamentos";

        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Medicamento med = new Medicamento();
                med.setId(rs.getLong("id"));
                med.setNome(rs.getString("nome"));
                med.setDosagem(rs.getString("dosagem"));
                med.setHorario(rs.getString("horario"));
                
                medicamentos.add(med);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar medicamentos: " + e.getMessage());
        }

        return medicamentos;
    }
}