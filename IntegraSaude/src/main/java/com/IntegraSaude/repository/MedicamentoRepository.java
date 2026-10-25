package com.IntegraSaude.repository;

import com.IntegraSaude.model.Medicamento;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A classe MedicamentoRepository gere a comunicação com o base de dados
 * para realizar operações de cadastro e consulta de medicamentos.
 */
public class MedicamentoRepository {

    /**
     * Insere um novo medicamento no base de dados SQLite.
     * @param medicamento Objeto contendo os dados a serem salvos
     */
    public void inserir(Medicamento medicamento) {
        String sql = "INSERT INTO medicamentos (paciente_id, nome, dosagem, frequencia_horas, primeiro_horario, data_inicio, data_fim) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Conexao.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, medicamento.getPacienteId());
            pstmt.setString(2, medicamento.getNome());
            pstmt.setString(3, medicamento.getDosagem());
            pstmt.setInt(4, medicamento.getFrequenciaHoras());
            
            // Converte LocalTime para String (formato ISO-8601)
            pstmt.setString(5, medicamento.getPrimeiroHorario() != null ? medicamento.getPrimeiroHorario().toString() : null);
            
            // Converte LocalDate para String (data de início)
            pstmt.setString(6, medicamento.getDataInicio() != null ? medicamento.getDataInicio().toString() : null);
            
            // Converte LocalDate para String (data de fim)
            pstmt.setString(7, medicamento.getDataFim() != null ? medicamento.getDataFim().toString() : null);

            pstmt.executeUpdate();
            System.out.println("-> Medicamento cadastrado com sucesso no base de dados!");

        } catch (SQLException e) {
            System.err.println("Erro ao inserir medicamento: " + e.getMessage());
        }
    }

    /**
     * Lista todos os medicamentos cadastrados no base de dados.
     * @return Uma lista com todos os medicamentos encontrados
     */
    public List<Medicamento> listarTodos() {
        List<Medicamento> medicamentos = new ArrayList<>();
        String sql = "SELECT id, paciente_id, nome, dosagem, frequencia_horas, primeiro_horario, data_inicio, data_fim FROM medicamentos";

        try (Connection conn = Conexao.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Medicamento med = new Medicamento();
                med.setId(rs.getLong("id"));
                med.setPacienteId(rs.getLong("paciente_id"));
                med.setNome(rs.getString("nome"));
                med.setDosagem(rs.getString("dosagem"));
                med.setFrequenciaHoras(rs.getInt("frequencia_horas"));

                // Converte a String do base de dados para LocalTime
                String horarioStr = rs.getString("primeiro_horario");
                if (horarioStr != null && !horarioStr.isEmpty()) {
                    med.setPrimeiroHorario(LocalTime.parse(horarioStr));
                }

                // Converte a String do base de dados para LocalDate (data de início)
                String dataInicioStr = rs.getString("data_inicio");
                if (dataInicioStr != null && !dataInicioStr.isEmpty()) {
                    med.setDataInicio(LocalDate.parse(dataInicioStr));
                }

                // Converte a String do base de dados para LocalDate (data de fim)
                String dataFimStr = rs.getString("data_fim");
                if (dataFimStr != null && !dataFimStr.isEmpty()) {
                    med.setDataFim(LocalDate.parse(dataFimStr));
                }

                medicamentos.add(med);
            }

        } catch (SQLException e) {
            System.err.println("Erro ao listar medicamentos: " + e.getMessage());
        }

        return medicamentos;
    }
}