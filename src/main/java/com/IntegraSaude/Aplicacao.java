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
import com.IntegraSaude.service.Servicos;
import com.IntegraSaude.service.SessaoUsuario;
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
import com.IntegraSaude.web.ServidorWeb;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

/**
 * Monta todas as dependências (a "fiação" do sistema), inicia o agendador e abre o menu de console
 * ou, com --web, o servidor do frontend. É aqui que se troca repositório em memória por SQLite.
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

    // Cria serviços, agendador e dados de demonstração com os repositórios recebidos
    private static void rodar(ConfiguracaoApp configuracao, Repositorios repos) {
        Clock relogio = Clock.systemDefaultZone();
        SaidaConsole saida = new SaidaConsole(System.out);
        NotificadorConsole notificador = new NotificadorConsole(saida);
        Servicos servicos = Servicos.montar(repos, configuracao.lembretes(), relogio, notificador,
                new ViaCepClient());

        // Agendador em segundo plano
        VerificadorLembretes verificador = new VerificadorLembretes(servicos.usuarios(), servicos.gerador(),
                servicos.lembretes(), servicos.alertas(), notificador);
        AgendadorLembretes agendador = new AgendadorLembretes(verificador::verificar, relogio,
                configuracao.intervaloAgendadorSegundos());

        DadosDemonstracao demonstracao = new DadosDemonstracao(servicos.usuarios(), servicos.vinculos(),
                servicos.medicamentos(), servicos.especialidades(), servicos.medicos(), servicos.consultas(),
                relogio);
        if (configuracao.carregarDemonstracao()) {
            saida.exibir(demonstracao.carregar());
        }
        if (configuracao.modoWeb()) {
            executarWeb(configuracao, saida, servicos, agendador);
        } else {
            executarMenu(saida, servicos, demonstracao, agendador);
        }
    }

    // Modo --web: sobe o servidor HTTP e fica rodando até ENTER ou Ctrl+C
    private static void executarWeb(ConfiguracaoApp configuracao, SaidaConsole saida, Servicos servicos,
                                    AgendadorLembretes agendador) {
        ServidorWeb servidor = new ServidorWeb(servicos, configuracao.portaWeb());
        CountDownLatch encerrar = new CountDownLatch(1);
        Thread ganchoDesligamento = new Thread(() -> {
            servidor.parar();
            agendador.parar();
        }, "desligamento-web");
        Runtime.getRuntime().addShutdownHook(ganchoDesligamento);

        agendador.iniciar();
        servidor.iniciar();
        saida.exibir("IntegraSaude web rodando em " + servidor.endereco());
        saida.exibir("Pressione ENTER (ou Ctrl+C) para encerrar.");
        aguardarEnter(encerrar);
        try {
            encerrar.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Runtime.getRuntime().removeShutdownHook(ganchoDesligamento);
        servidor.parar();
        agendador.parar();
        saida.exibir("Servidor encerrado. Ate logo!");
    }

    // Lê o teclado em segundo plano; sem entrada (ex.: rodando em segundo plano) segue até o Ctrl+C
    private static void aguardarEnter(CountDownLatch encerrar) {
        Thread leitor = new Thread(() -> {
            try {
                BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                if (teclado.readLine() != null) {
                    encerrar.countDown();
                }
            } catch (IOException e) {
                // Sem teclado disponível: o servidor continua até o Ctrl+C
            }
        }, "leitor-enter");
        leitor.setDaemon(true);
        leitor.start();
    }

    // Monta os menus, inicia o agendador, abre o menu e garante o shutdown no fim
    private static void executarMenu(SaidaConsole saida, Servicos servicos, DadosDemonstracao demonstracao,
                                     AgendadorLembretes agendador) {
        LeitorConsole leitor = new LeitorConsole(new Scanner(System.in), saida);
        SessaoUsuario sessao = new SessaoUsuario();
        SelecaoPaciente selecao = new SelecaoPaciente(sessao, servicos.vinculos(), leitor, saida);
        MenuPrincipal menu = new MenuPrincipal(leitor, saida, sessao,
                new MenuUsuarios(leitor, saida, servicos.usuarios(), servicos.autenticacao(),
                        servicos.vinculos(), sessao),
                new MenuMedicamentos(leitor, saida, servicos.medicamentos(), sessao),
                new MenuConsultas(leitor, saida, servicos.consultas(), servicos.especialidades(),
                        servicos.medicos(), servicos.enderecos(), sessao),
                new MenuLembretes(leitor, saida, servicos.lembretes(), servicos.historico(), sessao, selecao),
                new MenuAgenda(leitor, saida, servicos.agenda(), selecao), demonstracao);

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
