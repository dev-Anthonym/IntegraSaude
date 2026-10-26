package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.repository.MedicoRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Médicos no SQLite (com JOIN em especialidade para montar o objeto completo). */
public class MedicoRepositoryJdbc extends RepositorioJdbc implements MedicoRepository {

    private static final String CONSULTA = "SELECT m.id, m.nome, e.id AS esp_id, e.nome AS esp_nome "
            + "FROM medico m JOIN especialidade e ON e.id = m.especialidade_id ";

    public MedicoRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Medico salvar(Medico medico) {
        long id = inserir("INSERT INTO medico (nome, especialidade_id) VALUES (?, ?)",
                medico.getNome(), medico.getEspecialidade().getId());
        return comId(medico, id);
    }

    @Override
    public Optional<Medico> buscarPorId(Long id) {
        return consultarUm(CONSULTA + "WHERE m.id = ?", this::mapear, id);
    }

    @Override
    public List<Medico> listarPorEspecialidade(Long especialidadeId) {
        return consultar(CONSULTA + "WHERE e.id = ? ORDER BY m.nome", this::mapear, especialidadeId);
    }

    // Reutilizado pelo repositório de consultas
    static Medico mapearMedico(ResultSet linha) throws SQLException {
        Especialidade especialidade = comId(new Especialidade(linha.getString("esp_nome")), linha.getLong("esp_id"));
        return comId(new Medico(linha.getString("medico_nome"), especialidade), linha.getLong("medico_id"));
    }

    private Medico mapear(ResultSet linha) throws SQLException {
        Especialidade especialidade = comId(new Especialidade(linha.getString("esp_nome")), linha.getLong("esp_id"));
        return comId(new Medico(linha.getString("nome"), especialidade), linha.getLong("id"));
    }
}
