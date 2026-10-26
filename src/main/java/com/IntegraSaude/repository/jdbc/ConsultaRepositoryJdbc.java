package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.StatusConsulta;
import com.IntegraSaude.repository.ConsultaRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Consultas no SQLite (JOIN com médico e especialidade).
 * O banco tem um índice único (paciente, data_hora) só para status AGENDADA.
 */
public class ConsultaRepositoryJdbc extends RepositorioJdbc implements ConsultaRepository {

    private static final String CONSULTA = "SELECT c.id, c.paciente_id, c.local, c.data_hora, c.status, "
            + "m.id AS medico_id, m.nome AS medico_nome, e.id AS esp_id, e.nome AS esp_nome "
            + "FROM consulta c JOIN medico m ON m.id = c.medico_id "
            + "JOIN especialidade e ON e.id = m.especialidade_id ";

    public ConsultaRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Consulta salvar(Consulta c) {
        long id = inserir("INSERT INTO consulta (paciente_id, medico_id, local, data_hora, status) "
                        + "VALUES (?, ?, ?, ?, ?)",
                c.getPacienteId(), c.getMedico().getId(), c.getLocal(), c.getDataHora(), c.getStatus());
        return comId(c, id);
    }

    @Override
    public void atualizar(Consulta c) {
        executar("UPDATE consulta SET medico_id = ?, local = ?, data_hora = ?, status = ? WHERE id = ?",
                c.getMedico().getId(), c.getLocal(), c.getDataHora(), c.getStatus(), c.getId());
    }

    @Override
    public Optional<Consulta> buscarPorId(Long id) {
        return consultarUm(CONSULTA + "WHERE c.id = ?", this::mapear, id);
    }

    @Override
    public Optional<Consulta> buscarPorPacienteEHorario(Long pacienteId, LocalDateTime dataHora) {
        return consultarUm(CONSULTA + "WHERE c.paciente_id = ? AND c.data_hora = ? AND c.status = ?",
                this::mapear, pacienteId, dataHora, StatusConsulta.AGENDADA);
    }

    @Override
    public List<Consulta> listarPorPaciente(Long pacienteId) {
        return consultar(CONSULTA + "WHERE c.paciente_id = ? AND c.status <> ? ORDER BY c.data_hora",
                this::mapear, pacienteId, StatusConsulta.CANCELADA);
    }

    private Consulta mapear(ResultSet linha) throws SQLException {
        return comId(new Consulta(linha.getLong("paciente_id"), MedicoRepositoryJdbc.mapearMedico(linha),
                linha.getString("local"), LocalDateTime.parse(linha.getString("data_hora")),
                StatusConsulta.valueOf(linha.getString("status"))), linha.getLong("id"));
    }
}
