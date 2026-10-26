package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.exception.AppException;
import com.IntegraSaude.exception.PersistenciaException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.EntidadeBase;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base dos repositórios JDBC. Concentra o que se repete:
 * try-with-resources, conversão de tipos e tradução de SQLException para exceção da aplicação.
 * Usa uma única conexão compartilhada, por isso cada operação é sincronizada nela.
 */
abstract class RepositorioJdbc {

    // Formatos ISO-8601 sempre com segundos (toString() omitiria os segundos zerados e quebraria a ordenação)
    static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ISO_LOCAL_TIME;
    private static final String MARCA_UNICIDADE = "UNIQUE constraint failed";
    private static final int TAMANHO_DATA_ISO = 10;

    /** Converte uma linha do ResultSet em objeto. */
    @FunctionalInterface
    interface MapeadorLinha<T> {
        T mapear(ResultSet linha) throws SQLException;
    }

    protected final Connection conexao;

    protected RepositorioJdbc(Connection conexao) {
        this.conexao = conexao;
    }

    // SELECT que devolve uma lista
    protected <T> List<T> consultar(String sql, MapeadorLinha<T> mapeador, Object... parametros) {
        synchronized (conexao) {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                vincular(comando, parametros);
                try (ResultSet resultado = comando.executeQuery()) {
                    List<T> lista = new ArrayList<>();
                    while (resultado.next()) {
                        lista.add(mapeador.mapear(resultado));
                    }
                    return lista;
                }
            } catch (SQLException e) {
                throw traduzir(e);
            }
        }
    }

    // SELECT que devolve no máximo um objeto
    protected <T> Optional<T> consultarUm(String sql, MapeadorLinha<T> mapeador, Object... parametros) {
        return consultar(sql, mapeador, parametros).stream().findFirst();
    }

    // INSERT que devolve o id gerado pelo banco
    protected long inserir(String sql, Object... parametros) {
        synchronized (conexao) {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                vincular(comando, parametros);
                comando.executeUpdate();
                try (Statement consulta = conexao.createStatement();
                     ResultSet resultado = consulta.executeQuery("SELECT last_insert_rowid()")) {
                    resultado.next();
                    return resultado.getLong(1);
                }
            } catch (SQLException e) {
                throw traduzir(e);
            }
        }
    }

    // UPDATE ou DELETE; devolve quantas linhas foram afetadas
    protected int executar(String sql, Object... parametros) {
        synchronized (conexao) {
            try (PreparedStatement comando = conexao.prepareStatement(sql)) {
                vincular(comando, parametros);
                return comando.executeUpdate();
            } catch (SQLException e) {
                throw traduzir(e);
            }
        }
    }

    // Lê uma hora gravada como "08:00", "08:00:00" ou "2026-10-01T08:00:00" (formatos que já apareceram no banco)
    protected static LocalTime lerHora(String texto) {
        return texto.contains("T") ? LocalDateTime.parse(texto).toLocalTime() : LocalTime.parse(texto);
    }

    // Lê uma data gravada como "2026-10-01" ou "2026-10-01T00:00:00" (só os 10 primeiros caracteres importam)
    protected static LocalDate lerData(String texto) {
        return LocalDate.parse(texto.length() > TAMANHO_DATA_ISO ? texto.substring(0, TAMANHO_DATA_ISO) : texto);
    }

    // Atribui o id gerado à entidade e a devolve
    protected static <T extends EntidadeBase> T comId(T entidade, long id) {
        entidade.setId(id);
        return entidade;
    }

    // Converte cada parâmetro Java para o formato guardado no SQLite
    private void vincular(PreparedStatement comando, Object[] parametros) throws SQLException {
        for (int i = 0; i < parametros.length; i++) {
            comando.setObject(i + 1, converter(parametros[i]));
        }
    }

    private Object converter(Object valor) {
        return switch (valor) {
            case null -> null;
            case LocalDateTime dataHora -> dataHora.truncatedTo(ChronoUnit.SECONDS).format(FORMATO_DATA_HORA);
            case LocalDate data -> data.toString();
            case LocalTime hora -> hora.truncatedTo(ChronoUnit.SECONDS).format(FORMATO_HORA);
            case Enum<?> constante -> constante.name();
            case Boolean verdadeiro -> verdadeiro ? 1 : 0;
            default -> valor;
        };
    }

    // SQLException nunca sobe para o menu: vira exceção da aplicação
    private AppException traduzir(SQLException e) {
        String mensagem = e.getMessage() == null ? "" : e.getMessage();
        if (mensagem.contains(MARCA_UNICIDADE)) {
            return new RegistroDuplicadoException("Registro duplicado", e);
        }
        return new PersistenciaException("Falha ao acessar o banco de dados", e);
    }
}
