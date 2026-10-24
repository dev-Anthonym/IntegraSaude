package com.IntegraSaude.repository;

import com.IntegraSaude.exception.PersistenciaException;
import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.TipoEvento;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// Persistência do histórico de eventos dos lembretes (tabela historico)
public class HistoricoRepositoryJdbc implements HistoricoRepository {

    private static final String SQL_INSERIR =
            "INSERT INTO historico (lembrete_id, tipo_evento, data_hora) VALUES (?, ?, ?)";
    private static final String SQL_LISTAR_POR_LEMBRETE = "SELECT id, lembrete_id, tipo_evento, data_hora "
            + "FROM historico WHERE lembrete_id = ? ORDER BY data_hora, id";

    private final FabricaConexao fabricaConexao;

    public HistoricoRepositoryJdbc(FabricaConexao fabricaConexao) {
        this.fabricaConexao = fabricaConexao;
    }

    @Override
    public EventoHistorico salvar(EventoHistorico evento) {
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            comando.setLong(1, evento.getLembreteId());
            comando.setString(2, evento.getTipoEvento().name());
            comando.setObject(3, evento.getDataHora());
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                chaves.next();
                evento.setId(chaves.getLong(1));
            }
            return evento;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível registrar o evento no histórico.", e);
        }
    }

    @Override
    public List<EventoHistorico> listarPorLembrete(Long lembreteId) {
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_LISTAR_POR_LEMBRETE)) {
            comando.setLong(1, lembreteId);
            return mapearTodos(comando);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível consultar o histórico do lembrete " + lembreteId + ".", e);
        }
    }

    private List<EventoHistorico> mapearTodos(PreparedStatement comando) throws SQLException {
        List<EventoHistorico> eventos = new ArrayList<>();
        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                eventos.add(new EventoHistorico(resultado.getLong("id"), resultado.getLong("lembrete_id"),
                        TipoEvento.valueOf(resultado.getString("tipo_evento")),
                        resultado.getObject("data_hora", LocalDateTime.class)));
            }
        }
        return eventos;
    }
}
