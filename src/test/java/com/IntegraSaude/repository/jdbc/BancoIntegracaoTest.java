package com.IntegraSaude.repository.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.PersistenciaException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.StatusConsulta;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integração do código com o banco SQLite do projeto (scripts da pasta database/).
 * Cada teste usa um arquivo temporário próprio, então nada toca o banco real.
 */
class BancoIntegracaoTest {

    // Cria um arquivo temporário para o banco
    private File novoArquivo() throws IOException {
        File arquivo = File.createTempFile("integrasaude-teste", ".db");
        arquivo.deleteOnExit();
        return arquivo;
    }

    // Abre o arquivo e cria as tabelas com os scripts do projeto
    private Connection abrirBanco(File arquivo) {
        Connection conexao = ConexaoBanco.abrirEmArquivoTemporario(arquivo.getAbsolutePath());
        InicializadorBanco.executar(conexao);
        return conexao;
    }

    // Monta o sistema inteiro usando os repositórios JDBC desta conexão
    private CenarioTeste cenario(Connection conexao, ConfiguracaoLembretes configuracao) {
        return new CenarioTeste(configuracao, RepositoriosJdbc.criar(conexao));
    }

    private int contar(Connection conexao, String sql) throws SQLException {
        try (Statement st = conexao.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void deveCriarAsOitoTabelasEAceitarInicializarDeNovo() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            InicializadorBanco.executar(conexao);
            assertEquals(8, contar(conexao, "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' "
                    + "AND name NOT LIKE 'sqlite_%'"));
            // O script 05 acrescenta a coluna telefone, e rodar de novo não pode falhar
            assertEquals(1, contar(conexao, "SELECT COUNT(*) FROM pragma_table_info('usuario') "
                    + "WHERE name = 'telefone'"));
        }
    }

    @Test
    void deveRejeitarEmailDuplicadoNoBanco() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            Repositorios repos = RepositoriosJdbc.criar(conexao);
            repos.usuarios().salvar(new Usuario("Ana", "ana@email.com", "hash", Perfil.PACIENTE));
            assertThrows(RegistroDuplicadoException.class,
                    () -> repos.usuarios().salvar(new Usuario("Outra", "ana@email.com", "hash", Perfil.PACIENTE)));
        }
    }

    @Test
    void deveConverterViolacaoDeChaveEstrangeiraEmExcecaoDaAplicacao() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            Repositorios repos = RepositoriosJdbc.criar(conexao);
            Medicamento orfao = new CenarioTeste().medicamento(999L, 8, LocalTime.of(8, 0));
            assertThrows(PersistenciaException.class, () -> repos.medicamentos().salvar(orfao));
        }
    }

    @Test
    void devePersistirDadosDepoisDeFecharEReabrirOBanco() throws Exception {
        File arquivo = novoArquivo();
        try (Connection conexao = abrirBanco(arquivo)) {
            cenario(conexao, ConfiguracaoLembretes.padrao()).criarPaciente("p@email.com");
        }
        // Reabre o mesmo arquivo: o usuário e a senha (hash) continuam lá
        try (Connection conexao = abrirBanco(arquivo)) {
            CenarioTeste c = cenario(conexao, ConfiguracaoLembretes.padrao());
            Usuario logado = c.autenticacao.login("p@email.com", CenarioTeste.SENHA_VALIDA);
            assertEquals(Perfil.PACIENTE, logado.getPerfil());
        }
    }

    @Test
    void deveRodarOFluxoCompletoDeLembreteEAlertaComSqlite() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            CenarioTeste c = cenario(conexao, new ConfiguracaoLembretes(30, 60));
            Usuario paciente = c.criarPaciente("p@email.com");
            c.vinculos.criar(paciente.getId(), c.criarResponsavel("r@email.com").getId());
            c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(12, 0)));

            // Gera o lembrete, deixa vencer e alerta o responsável
            Lembrete gerado = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).get(0);
            c.lembretes.verificarPerdidos(gerado.getHorarioPrevisto().plusMinutes(31));
            Lembrete perdido = c.repos.lembretes().buscarPorId(gerado.getId()).orElseThrow();
            assertEquals(1, c.alertas.alertarResponsaveis(perdido));
            assertEquals(0, c.alertas.alertarResponsaveis(perdido));

            // Relê do banco: status, dono (obtido por JOIN), descrição e flag alerta_enviado
            Lembrete doBanco = c.repos.lembretes().buscarPorId(gerado.getId()).orElseThrow();
            assertEquals(StatusLembrete.PERDIDO, doBanco.getStatus());
            assertTrue(doBanco.isAlertaEnviado());
            assertEquals(paciente.getId(), doBanco.getPacienteId());
            assertEquals("Tomar Dipirona (500 mg)", doBanco.getDescricao());

            // Histórico: gerado, perdido e alerta enviado, na ordem
            List<EventoHistorico> eventos = c.historico.listarDoPaciente(paciente.getId());
            assertEquals(3, eventos.size());
            assertEquals(TipoEvento.GERADO, eventos.get(0).getTipo());
            assertEquals(TipoEvento.PERDIDO, eventos.get(1).getTipo());
            assertEquals(TipoEvento.ALERTA_ENVIADO, eventos.get(2).getTipo());
            assertEquals("Perdido: Tomar Dipirona (500 mg)", eventos.get(1).getDescricao());
        }
    }

    @Test
    void naoDeveGerarLembreteDuplicadoComSqlite() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            CenarioTeste c = cenario(conexao, new ConfiguracaoLembretes(30, 60));
            Usuario paciente = c.criarPaciente("p@email.com");
            c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(12, 0)));
            c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
            assertTrue(c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).isEmpty());
            assertEquals(1, c.lembretes.listarPendentes(paciente.getId()).size());
        }
    }

    @Test
    void deveCancelarConsultaSemApagarELiberarOHorario() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            CenarioTeste c = cenario(conexao, ConfiguracaoLembretes.padrao());
            Usuario paciente = c.criarPaciente("p@email.com");
            Medico medico = c.criarMedico();
            LocalDateTime horario = CenarioTeste.AGORA.plusDays(2);
            Consulta salva = c.consultas.cadastrar(c.consulta(paciente.getId(), medico, horario));

            c.consultas.cancelar(paciente.getId(), salva.getId());

            // Some da lista, mas continua no banco com status CANCELADA
            assertTrue(c.consultas.listarDoPaciente(paciente.getId()).isEmpty());
            assertEquals(StatusConsulta.CANCELADA,
                    c.repos.consultas().buscarPorId(salva.getId()).orElseThrow().getStatus());
            // O horário fica livre para uma nova consulta (índice único só vale para AGENDADA)
            c.consultas.cadastrar(c.consulta(paciente.getId(), medico, horario));
            assertEquals(1, c.consultas.listarDoPaciente(paciente.getId()).size());
        }
    }

    @Test
    void deveRejeitarDuasConsultasAgendadasNoMesmoHorarioNoBanco() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            CenarioTeste c = cenario(conexao, ConfiguracaoLembretes.padrao());
            Usuario paciente = c.criarPaciente("p@email.com");
            Medico medico = c.criarMedico();
            LocalDateTime horario = CenarioTeste.AGORA.plusDays(2);
            c.repos.consultas().salvar(c.consulta(paciente.getId(), medico, horario));
            assertThrows(RegistroDuplicadoException.class,
                    () -> c.repos.consultas().salvar(c.consulta(paciente.getId(), medico, horario)));
        }
    }

    @Test
    void deveRemoverMedicamentoELembretesSemViolarChaveEstrangeira() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            CenarioTeste c = cenario(conexao, new ConfiguracaoLembretes(30, 60));
            Usuario paciente = c.criarPaciente("p@email.com");
            Medicamento med = c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(12, 0)));
            c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
            c.medicamentos.remover(paciente.getId(), med.getId());
            assertTrue(c.medicamentos.listarDoPaciente(paciente.getId()).isEmpty());
            assertTrue(c.lembretes.listarPendentes(paciente.getId()).isEmpty());
        }
    }

    @Test
    void deveLerOsDadosDeDemonstracaoDoScriptDoBanco() throws Exception {
        try (Connection conexao = abrirBanco(novoArquivo())) {
            // Carrega o script 06 do colega (datas dos medicamentos gravadas como data e hora completas)
            InicializadorBanco.executarScript(conexao, "06_dados_demonstracao.sql");
            Repositorios repos = RepositoriosJdbc.criar(conexao);

            assertTrue(repos.usuarios().buscarPorEmail("maria@exemplo.com").isPresent());
            List<Medicamento> medicamentos = repos.medicamentos().listarPorPaciente(1L);
            assertEquals(2, medicamentos.size());
            assertEquals(LocalTime.of(7, 0), medicamentos.get(0).getPrimeiroHorario());

            Lembrete medicamento = repos.lembretes().buscarPorId(1L).orElseThrow();
            assertEquals("Tomar Paracetamol (500mg)", medicamento.getDescricao());
            assertEquals(Long.valueOf(1), medicamento.getPacienteId());
            assertTrue(medicamento.isAlertaEnviado());

            Lembrete consulta = repos.lembretes().buscarPorId(3L).orElseThrow();
            assertEquals("Consulta de Cardiologia com Dr. Roberto Carlos em Hospital Vida as 14:00",
                    consulta.getDescricao());

            assertEquals(4, repos.historico().listarPorPaciente(1L).size());
            assertFalse(repos.historico().listarPorPeriodo(1L,
                    LocalDateTime.of(2026, 10, 2, 8, 0), LocalDateTime.of(2026, 10, 2, 8, 10)).isEmpty());
        }
    }
}
