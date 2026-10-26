package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.DescricaoLembrete;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.LembreteRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lembretes no SQLite. No banco, o lembrete aponta para um medicamento OU para uma consulta
 * (colunas medicamento_id e consulta_id) e não guarda paciente nem texto. Por isso as consultas
 * fazem JOIN para descobrir o paciente (dono do medicamento/consulta) e montar a descrição.
 */
public class LembreteRepositoryJdbc extends RepositorioJdbc implements LembreteRepository {

    // Colunas que o mapeador precisa (paciente e peças da descrição vêm dos JOINs)
    static final String COLUNAS = "SELECT l.id, l.tipo, COALESCE(l.medicamento_id, l.consulta_id) AS item_id, "
            + "l.horario_previsto, l.status, l.alerta_enviado, "
            + "COALESCE(m.paciente_id, c.paciente_id) AS paciente_id, "
            + "m.nome AS med_nome, m.dosagem AS med_dosagem, "
            + "c.local AS con_local, c.data_hora AS con_data_hora, md.nome AS medico_nome, e.nome AS esp_nome ";

    // JOINs (LEFT, porque só um dos dois lados existe em cada lembrete)
    static final String JUNCOES = "LEFT JOIN medicamento m ON m.id = l.medicamento_id "
            + "LEFT JOIN consulta c ON c.id = l.consulta_id "
            + "LEFT JOIN medico md ON md.id = c.medico_id "
            + "LEFT JOIN especialidade e ON e.id = md.especialidade_id ";

    private static final String CONSULTA = COLUNAS + "FROM lembrete l " + JUNCOES;

    public LembreteRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public Lembrete salvar(Lembrete l) {
        // Respeita o CHECK do banco: medicamento_id preenchido OU consulta_id preenchido
        boolean ehMedicamento = l.getTipo() == TipoLembrete.MEDICAMENTO;
        long id = inserir("INSERT INTO lembrete (tipo, medicamento_id, consulta_id, horario_previsto, status, "
                        + "alerta_enviado) VALUES (?, ?, ?, ?, ?, ?)",
                l.getTipo(), ehMedicamento ? l.getItemId() : null, ehMedicamento ? null : l.getItemId(),
                l.getHorarioPrevisto(), l.getStatus(), l.isAlertaEnviado());
        return comId(l, id);
    }

    @Override
    public void atualizar(Lembrete l) {
        executar("UPDATE lembrete SET status = ?, alerta_enviado = ? WHERE id = ?",
                l.getStatus(), l.isAlertaEnviado(), l.getId());
    }

    @Override
    public Optional<Lembrete> buscarPorId(Long id) {
        return consultarUm(CONSULTA + "WHERE l.id = ?", this::mapear, id);
    }

    @Override
    public boolean existe(Long pacienteId, TipoLembrete tipo, Long itemId, LocalDateTime horarioPrevisto) {
        return consultarUm("SELECT 1 AS achou FROM lembrete WHERE tipo = ? "
                        + "AND COALESCE(medicamento_id, consulta_id) = ? AND horario_previsto = ?",
                linha -> linha.getInt("achou"), tipo, itemId, horarioPrevisto).isPresent();
    }

    @Override
    public List<Lembrete> listarPendentesDoPaciente(Long pacienteId) {
        return consultar(CONSULTA + "WHERE l.status = ? AND COALESCE(m.paciente_id, c.paciente_id) = ? "
                + "ORDER BY l.horario_previsto", this::mapear, StatusLembrete.PENDENTE, pacienteId);
    }

    @Override
    public List<Lembrete> listarPendentesAte(LocalDateTime limite) {
        return consultar(CONSULTA + "WHERE l.status = ? AND l.horario_previsto <= ? "
                + "ORDER BY l.horario_previsto", this::mapear, StatusLembrete.PENDENTE, limite);
    }

    @Override
    public void removerPendentesDoItem(TipoLembrete tipo, Long itemId) {
        executar("DELETE FROM lembrete WHERE status = ? AND tipo = ? "
                + "AND COALESCE(medicamento_id, consulta_id) = ?", StatusLembrete.PENDENTE, tipo, itemId);
    }

    // Monta o texto do lembrete a partir das colunas dos JOINs (usado também pelo histórico)
    static String descrever(ResultSet linha, TipoLembrete tipo) throws SQLException {
        if (tipo == TipoLembrete.MEDICAMENTO) {
            return DescricaoLembrete.medicamento(linha.getString("med_nome"), linha.getString("med_dosagem"));
        }
        return DescricaoLembrete.consulta(linha.getString("esp_nome"), linha.getString("medico_nome"),
                linha.getString("con_local"), LocalDateTime.parse(linha.getString("con_data_hora")));
    }

    private Lembrete mapear(ResultSet linha) throws SQLException {
        TipoLembrete tipo = TipoLembrete.valueOf(linha.getString("tipo"));
        return comId(new Lembrete(linha.getLong("paciente_id"), tipo, linha.getLong("item_id"),
                LocalDateTime.parse(linha.getString("horario_previsto")), descrever(linha, tipo),
                StatusLembrete.valueOf(linha.getString("status")), linha.getInt("alerta_enviado") == 1),
                linha.getLong("id"));
    }
}
