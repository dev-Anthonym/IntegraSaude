package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.repository.EspecialidadeRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Especialidades no SQLite. */
public class EspecialidadeRepositoryJdbc extends RepositorioJdbc implements EspecialidadeRepository {

    public EspecialidadeRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Especialidade salvar(Especialidade especialidade) {
        long id = inserir("INSERT INTO especialidade (nome) VALUES (?)", especialidade.getNome());
        return comId(especialidade, id);
    }

    @Override
    public Optional<Especialidade> buscarPorId(Long id) {
        return consultarUm("SELECT id, nome FROM especialidade WHERE id = ?", this::mapear, id);
    }

    @Override
    public List<Especialidade> listar() {
        return consultar("SELECT id, nome FROM especialidade ORDER BY nome", this::mapear);
    }

    private Especialidade mapear(ResultSet linha) throws SQLException {
        return comId(new Especialidade(linha.getString("nome")), linha.getLong("id"));
    }
}
