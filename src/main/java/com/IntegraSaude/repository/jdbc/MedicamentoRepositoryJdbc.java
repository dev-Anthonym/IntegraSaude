package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.repository.MedicamentoRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Medicamentos no SQLite. Datas e horas são gravadas como texto ISO-8601. */
public class MedicamentoRepositoryJdbc extends RepositorioJdbc implements MedicamentoRepository {

    private static final String COLUNAS = "SELECT id, paciente_id, nome, dosagem, frequencia_horas, "
            + "primeiro_horario, data_inicio, data_fim FROM medicamento ";

    public MedicamentoRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Medicamento salvar(Medicamento m) {
        long id = inserir("INSERT INTO medicamento (paciente_id, nome, dosagem, frequencia_horas, "
                        + "primeiro_horario, data_inicio, data_fim) VALUES (?, ?, ?, ?, ?, ?, ?)",
                m.getPacienteId(), m.getNome(), m.getDosagem(), m.getFrequenciaHoras(),
                m.getPrimeiroHorario(), m.getDataInicio(), m.getDataFim());
        return comId(m, id);
    }

    @Override
    public void atualizar(Medicamento m) {
        executar("UPDATE medicamento SET nome = ?, dosagem = ?, frequencia_horas = ?, primeiro_horario = ?, "
                        + "data_inicio = ?, data_fim = ? WHERE id = ?",
                m.getNome(), m.getDosagem(), m.getFrequenciaHoras(), m.getPrimeiroHorario(),
                m.getDataInicio(), m.getDataFim(), m.getId());
    }

    @Override
    public Optional<Medicamento> buscarPorId(Long id) {
        return consultarUm(COLUNAS + "WHERE id = ?", this::mapear, id);
    }

    @Override
    public List<Medicamento> listarPorPaciente(Long pacienteId) {
        return consultar(COLUNAS + "WHERE paciente_id = ? ORDER BY nome", this::mapear, pacienteId);
    }

    @Override
    public boolean remover(Long id) {
        return executar("DELETE FROM medicamento WHERE id = ?", id) > 0;
    }

    private Medicamento mapear(ResultSet linha) throws SQLException {
        return comId(new Medicamento(linha.getLong("paciente_id"), linha.getString("nome"),
                linha.getString("dosagem"), linha.getInt("frequencia_horas"),
                lerHora(linha.getString("primeiro_horario")),
                lerData(linha.getString("data_inicio")),
                lerData(linha.getString("data_fim"))), linha.getLong("id"));
    }
}
