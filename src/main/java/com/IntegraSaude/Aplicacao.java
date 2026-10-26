package com.IntegraSaude;

import com.IntegraSaude.config.ConfiguracaoApp;
import com.IntegraSaude.config.ConfiguracaoLog;
import com.IntegraSaude.exception.EntradaEncerradaException;
import com.IntegraSaude.exception.PersistenciaException;
import com.IntegraSaude.integracao.ViaCepClient;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;
import com.IntegraSaude.repository.jdbc.InicializadorBanco;
import com.IntegraSaude.repository.jdbc.RepositoriosJdbc;
import com.IntegraSaude.scheduler.AgendadorLembretes;
import com.IntegraSaude.scheduler.VerificadorLembretes;
import com.IntegraSaude.service.AgendaService;
import com.IntegraSaude.service.AlertaService;
import com.IntegraSaude.service.AutenticacaoService;
import com.IntegraSaude.service.CalculadoraHorarios;
import com.IntegraSaude.service.ConsultaService;
import com.IntegraSaude.service.EnderecoService;
import com.IntegraSaude.service.EspecialidadeService;
import com.IntegraSaude.service.GeradorLembretes;
import com.IntegraSaude.service.HistoricoService;
import com.IntegraSaude.service.LembreteService;
import com.IntegraSaude.service.MedicamentoService;
import com.IntegraSaude.service.MedicoService;
import com.IntegraSaude.service.SenhaService;
import com.IntegraSaude.service.SessaoUsuario;
import com.IntegraSaude.service.UsuarioService;
import com.IntegraSaude.service.ValidadorConsulta;
import com.IntegraSaude.service.ValidadorMedicamento;
import com.IntegraSaude.service.ValidadorUsuario;
import com.IntegraSaude.service.VinculoService;
import com.IntegraSaude.ui.DadosDemonstracao;
import com.IntegraSaude.ui.LeitorConsole;
import com.IntegraSaude.ui.MenuAgenda;
import com.IntegraSaude.ui.MenuConsultas;
import com.IntegraSaude.ui.MenuLembretes;
import com.IntegraSaude.ui.MenuMedicamentos;
import com.IntegraSaude.ui.MenuPrincipal;
import com.IntegraSaude.ui.MenuUsuarios;
import com.IntegraSaude.ui.NotificadorConsole;
import com.IntegraSaude.ui.SaidaConsole;
import com.IntegraSaude.ui.SelecaoPaciente;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Scanner;

/**
 * Monta todas as dependências (a "fiação" do sistema), inicia o agendador e abre o menu.
 * É aqui que se troca repositório em memória por SQLite.
 */
public final class Aplicacao {

    private Aplicacao() {
    }

    // Ponto de entrada: escolhe memória ou SQLite e roda
    public static void executar(ConfiguracaoApp configuracao) {
        ConfiguracaoLog.iniciar();
        if (configuracao.usarMemoria()) {
            rodar(configuracao, Repositorios.emMemoria());
            return;
        }
        // try-with-resources fecha a conexão ao sair
        try (Connection conexao = ConexaoBanco.abrir(configuracao.caminhoBanco())) {
            InicializadorBanco.executar(conexao);
            rodar(configuracao, RepositoriosJdbc.criar(conexao));
        } catch (SQLException e) {
            throw new PersistenciaException("Falha ao fechar o banco de dados", e);
        }
    }

    // Cria serviços, agendador e menus com os repositórios recebidos
    private static void rodar(ConfiguracaoApp configuracao, Repositorios repos) {
        Clock relogio = Clock.systemDefaultZone();
        SaidaConsole saida = new SaidaConsole(System.out);
        LeitorConsole leitor = new LeitorConsole(new Scanner(System.in), saida);
        NotificadorConsole notificador = new NotificadorConsole(saida);

        // Serviços de usuário
        SenhaService senhas = new SenhaService();
        UsuarioService usuarios = new UsuarioService(repos.usuarios(), new ValidadorUsuario(), senhas);
        AutenticacaoService autenticacao = new AutenticacaoService(repos.usuarios(), senhas);
        VinculoService vinculos = new VinculoService(repos.vinculos(), repos.usuarios());
        SessaoUsuario sessao = new SessaoUsuario();

        // Serviços de medicamentos, consultas e agenda
        CalculadoraHorarios calculadora = new CalculadoraHorarios();
        MedicamentoService medicamentos = new MedicamentoService(repos.medicamentos(), repos.usuarios(),
                repos.lembretes(), new ValidadorMedicamento(relogio));
        EspecialidadeService especialidades = new EspecialidadeService(repos.especialidades());
        MedicoService medicos = new MedicoService(repos.medicos(), repos.especialidades());
        ConsultaService consultas = new ConsultaService(repos.consultas(), repos.usuarios(),
                repos.lembretes(), new ValidadorConsulta(relogio), relogio);
        AgendaService agenda = new AgendaService(repos.medicamentos(), repos.consultas(), calculadora);

        // Serviços de lembretes, alerta e histórico
        HistoricoService historico = new HistoricoService(repos.historico(), relogio);
        LembreteService lembretes = new LembreteService(repos.lembretes(), historico, relogio,
                configuracao.lembretes());
        GeradorLembretes gerador = new GeradorLembretes(repos.medicamentos(), repos.consultas(),
                repos.lembretes(), calculadora, historico, relogio, configuracao.lembretes());
        AlertaService alertas = new AlertaService(repos.lembretes(), vinculos, usuarios, historico, notificador);

        // Agendador em segundo plano
        VerificadorLembretes verificador = new VerificadorLembretes(usuarios, gerador, lembretes, alertas, notificador);
        AgendadorLembretes agendador = new AgendadorLembretes(verificador::verificar, relogio,
                configuracao.intervaloAgendadorSegundos());

        // Menus
        SelecaoPaciente selecao = new SelecaoPaciente(sessao, vinculos, leitor, saida);
        DadosDemonstracao demonstracao = new DadosDemonstracao(usuarios, vinculos, medicamentos,
                especialidades, medicos, consultas, relogio);
        MenuPrincipal menu = new MenuPrincipal(leitor, saida, sessao,
                new MenuUsuarios(leitor, saida, usuarios, autenticacao, vinculos, sessao),
                new MenuMedicamentos(leitor, saida, medicamentos, sessao),
                new MenuConsultas(leitor, saida, consultas, especialidades, medicos,
                        new EnderecoService(new ViaCepClient()), sessao),
                new MenuLembretes(leitor, saida, lembretes, historico, sessao, selecao),
                new MenuAgenda(leitor, saida, agenda, selecao), demonstracao);

        executarMenu(configuracao, saida, demonstracao, agendador, menu);
    }

    // Carrega a demonstração (se pedido), inicia o agendador, abre o menu e garante o shutdown no fim
    private static void executarMenu(ConfiguracaoApp configuracao, SaidaConsole saida,
                                     DadosDemonstracao demonstracao, AgendadorLembretes agendador,
                                     MenuPrincipal menu) {
        if (configuracao.carregarDemonstracao()) {
            saida.exibir(demonstracao.carregar());
        }
        agendador.iniciar();
        try {
            menu.executar();
        } catch (EntradaEncerradaException e) {
            saida.exibir("Entrada encerrada. Ate logo!");
        } finally {
            agendador.parar();
        }
    }
}
