package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.Vinculo;
import com.IntegraSaude.repository.VinculoRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Vínculos no SQLite. */
public class VinculoRepositoryJdbc extends RepositorioJdbc implements VinculoRepository {

    private static final String COLUNAS = "SELECT id, paciente_id, responsavel_id FROM vinculo ";

    public VinculoRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Vinculo salvar(Vinculo vinculo) {
        long id = inserir("INSERT INTO vinculo (paciente_id, responsavel_id) VALUES (?, ?)",
                vinculo.getPacienteId(), vinculo.getResponsavelId());
        return comId(vinculo, id);
    }

    @Override
    public Optional<Vinculo> buscarPorPar(Long pacienteId, Long responsavelId) {
        return consultarUm(COLUNAS + "WHERE paciente_id = ? AND responsavel_id = ?",
                this::mapear, pacienteId, responsavelId);
    }

    @Override
    public List<Vinculo> listarPorPaciente(Long pacienteId) {
        return consultar(COLUNAS + "WHERE paciente_id = ? ORDER BY id", this::mapear, pacienteId);
    }

    @Override
    public List<Vinculo> listarPorResponsavel(Long responsavelId) {
        return consultar(COLUNAS + "WHERE responsavel_id = ? ORDER BY id", this::mapear, responsavelId);
    }

    @Override
    public boolean remover(Long id) {
        return executar("DELETE FROM vinculo WHERE id = ?", id) > 0;
    }

    private Vinculo mapear(ResultSet linha) throws SQLException {
        return comId(new Vinculo(linha.getLong("paciente_id"), linha.getLong("responsavel_id")),
                linha.getLong("id"));
    }
}
