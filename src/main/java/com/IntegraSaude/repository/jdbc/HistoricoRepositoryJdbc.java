package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.model.DescricaoLembrete;
import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.HistoricoRepository;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Histórico no SQLite. A tabela historico guarda só lembrete_id, tipo_evento e data_hora;
 * paciente e descrição são obtidos pelo JOIN com o lembrete (e com o medicamento/consulta dele).
 */
public class HistoricoRepositoryJdbc extends RepositorioJdbc implements HistoricoRepository {

    private static final String CONSULTA = "SELECT h.id, h.lembrete_id, h.tipo_evento, h.data_hora, l.tipo, "
            + "COALESCE(m.paciente_id, c.paciente_id) AS paciente_id, "
            + "m.nome AS med_nome, m.dosagem AS med_dosagem, "
            + "c.local AS con_local, c.data_hora AS con_data_hora, md.nome AS medico_nome, e.nome AS esp_nome "
            + "FROM historico h JOIN lembrete l ON l.id = h.lembrete_id " + LembreteRepositoryJdbc.JUNCOES;

    private static final String ORDEM = " ORDER BY h.data_hora, h.id";

    public HistoricoRepositoryJdbc(Connection conexao) {
        super(conexao);
    }

    @Override
    public EventoHistorico salvar(EventoHistorico e) {
        long id = inserir("INSERT INTO historico (lembrete_id, tipo_evento, data_hora) VALUES (?, ?, ?)",
                e.getLembreteId(), e.getTipo(), e.getDataHora());
        return comId(e, id);
    }

    @Override
    public List<EventoHistorico> listarPorPaciente(Long pacienteId) {
        return consultar(CONSULTA + "WHERE COALESCE(m.paciente_id, c.paciente_id) = ?" + ORDEM,
                this::mapear, pacienteId);
    }

    @Override
    public List<EventoHistorico> listarPorPeriodo(Long pacienteId, LocalDateTime de, LocalDateTime ate) {
        return consultar(CONSULTA + "WHERE COALESCE(m.paciente_id, c.paciente_id) = ? "
                + "AND h.data_hora >= ? AND h.data_hora <= ?" + ORDEM, this::mapear, pacienteId, de, ate);
    }

    @Override
    public boolean existeEvento(Long lembreteId, TipoEvento tipo) {
        return consultarUm("SELECT 1 AS achou FROM historico WHERE lembrete_id = ? AND tipo_evento = ?",
                linha -> linha.getInt("achou"), lembreteId, tipo).isPresent();
    }

    private EventoHistorico mapear(ResultSet linha) throws SQLException {
        TipoEvento tipoEvento = TipoEvento.valueOf(linha.getString("tipo_evento"));
        String descricaoLembrete = LembreteRepositoryJdbc.descrever(linha, TipoLembrete.valueOf(linha.getString("tipo")));
        return comId(new EventoHistorico(linha.getLong("paciente_id"), linha.getLong("lembrete_id"), tipoEvento,
                LocalDateTime.parse(linha.getString("data_hora")),
                DescricaoLembrete.evento(tipoEvento, descricaoLembrete)), linha.getLong("id"));
    }
}
