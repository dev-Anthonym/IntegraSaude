package com.IntegraSaude.repository;

import com.IntegraSaude.exception.PersistenciaException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoLembrete;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Persistência dos lembretes no banco relacional (tabela lembrete)
public class LembreteRepositoryJdbc implements LembreteRepository {

    private static final String SQL_INSERIR = "INSERT INTO lembrete (paciente_id, tipo, medicamento_id, "
            + "consulta_id, descricao, horario_previsto, status, alerta_enviado) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_ATUALIZAR = "UPDATE lembrete SET status = ?, alerta_enviado = ? WHERE id = ?";
    private static final String SQL_SELECIONAR = "SELECT id, paciente_id, tipo, medicamento_id, consulta_id, "
            + "descricao, horario_previsto, status, alerta_enviado FROM lembrete ";
    private static final String ORDENACAO = " ORDER BY horario_previsto, id";
    private static final String PENDENTE = StatusLembrete.PENDENTE.name();

    private final FabricaConexao fabricaConexao;

    public LembreteRepositoryJdbc(FabricaConexao fabricaConexao) {
        this.fabricaConexao = fabricaConexao;
    }

    @Override
    public Lembrete salvar(Lembrete lembrete) {
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_INSERIR, Statement.RETURN_GENERATED_KEYS)) {
            preencherInsercao(comando, lembrete);
            comando.executeUpdate();
            try (ResultSet chaves = comando.getGeneratedKeys()) {
                chaves.next();
                lembrete.setId(chaves.getLong(1));
            }
            return lembrete;
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível salvar o lembrete.", e);
        }
    }

    @Override
    public Optional<Lembrete> buscarPorId(Long id) {
        return listar(SQL_SELECIONAR + "WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<Lembrete> listarPendentesDoPaciente(Long pacienteId) {
        return listar(SQL_SELECIONAR + "WHERE paciente_id = ? AND status = ?" + ORDENACAO, pacienteId, PENDENTE);
    }

    @Override
    public List<Lembrete> listarPorPaciente(Long pacienteId) {
        return listar(SQL_SELECIONAR + "WHERE paciente_id = ?" + ORDENACAO, pacienteId);
    }

    @Override
    public List<Lembrete> listarPendentes() {
        return listar(SQL_SELECIONAR + "WHERE status = ?" + ORDENACAO, PENDENTE);
    }

    @Override
    public void atualizar(Lembrete lembrete) {
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(SQL_ATUALIZAR)) {
            comando.setString(1, lembrete.getStatus().name());
            comando.setBoolean(2, lembrete.isAlertaEnviado());
            comando.setLong(3, lembrete.getId());
            comando.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível atualizar o lembrete " + lembrete.getId() + ".", e);
        }
    }

    private void preencherInsercao(PreparedStatement comando, Lembrete lembrete) throws SQLException {
        boolean medicamento = lembrete.getTipo() == TipoLembrete.MEDICAMENTO;
        comando.setLong(1, lembrete.getPacienteId());
        comando.setString(2, lembrete.getTipo().name());
        definirIdOpcional(comando, 3, medicamento ? lembrete.getItemId() : null);
        definirIdOpcional(comando, 4, medicamento ? null : lembrete.getItemId());
        comando.setString(5, lembrete.getDescricao());
        comando.setObject(6, lembrete.getHorarioPrevisto());
        comando.setString(7, lembrete.getStatus().name());
        comando.setBoolean(8, lembrete.isAlertaEnviado());
    }

    private void definirIdOpcional(PreparedStatement comando, int posicao, Long id) throws SQLException {
        if (id == null) {
            comando.setNull(posicao, Types.BIGINT);
        } else {
            comando.setLong(posicao, id);
        }
    }

    private List<Lembrete> listar(String sql, Object... parametros) {
        try (Connection conexao = fabricaConexao.abrir();
             PreparedStatement comando = conexao.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                comando.setObject(i + 1, parametros[i]);
            }
            return mapearTodos(comando);
        } catch (SQLException e) {
            throw new PersistenciaException("Não foi possível consultar os lembretes.", e);
        }
    }

    private List<Lembrete> mapearTodos(PreparedStatement comando) throws SQLException {
        List<Lembrete> lembretes = new ArrayList<>();
        try (ResultSet resultado = comando.executeQuery()) {
            while (resultado.next()) {
                lembretes.add(mapear(resultado));
            }
        }
        return lembretes;
    }

    private Lembrete mapear(ResultSet resultado) throws SQLException {
        TipoLembrete tipo = TipoLembrete.valueOf(resultado.getString("tipo"));
        String colunaItem = tipo == TipoLembrete.MEDICAMENTO ? "medicamento_id" : "consulta_id";
        Lembrete lembrete = new Lembrete(resultado.getLong("id"), resultado.getLong("paciente_id"), tipo,
                resultado.getLong(colunaItem), resultado.getString("descricao"),
                resultado.getObject("horario_previsto", LocalDateTime.class));
        restaurarEstado(lembrete, StatusLembrete.valueOf(resultado.getString("status")),
                resultado.getBoolean("alerta_enviado"));
        return lembrete;
    }

    // Reaplica as transições da máquina de estados para recriar o lembrete gravado
    private void restaurarEstado(Lembrete lembrete, StatusLembrete status, boolean alertaEnviado) {
        if (status == StatusLembrete.CONFIRMADO) {
            lembrete.confirmar();
        } else if (status == StatusLembrete.PERDIDO) {
            lembrete.marcarComoPerdido();
        }
        if (alertaEnviado) {
            lembrete.marcarAlertaEnviado();
        }
    }
}
