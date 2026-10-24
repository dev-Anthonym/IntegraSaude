package com.IntegraSaude.ui;

import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.service.AgendadorLembretes;
import com.IntegraSaude.service.AlertaService;
import com.IntegraSaude.service.FonteConsultas;
import com.IntegraSaude.service.FonteMedicamentos;
import com.IntegraSaude.service.GeradorLembretes;
import com.IntegraSaude.service.HistoricoService;
import com.IntegraSaude.service.HorariosMedicamento;
import com.IntegraSaude.service.LembreteService;
import com.IntegraSaude.service.Notificador;
import com.IntegraSaude.service.ResponsaveisPorVinculo;
import com.IntegraSaude.service.VerificadorLembretes;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Duration;
import java.util.Scanner;

// Monta as dependências do módulo de lembretes e controla o agendador
public class ModuloLembretes {

    private final LembreteService lembreteService;
    private final HistoricoService historicoService;
    private final GeradorLembretes geradorLembretes;
    private final AgendadorLembretes agendador;
    private final Clock relogio;

    public ModuloLembretes(UsuarioRepository usuarioRepository, FonteMedicamentos fonteMedicamentos,
                           HorariosMedicamento horariosMedicamento, FonteConsultas fonteConsultas,
                           Notificador notificador, Clock relogio) {
        LembreteRepository lembreteRepository = new LembreteRepositoryMemoria();
        this.relogio = relogio;
        this.historicoService = new HistoricoService(new HistoricoRepositoryMemoria(), lembreteRepository, relogio);
        this.lembreteService = new LembreteService(lembreteRepository, historicoService, relogio,
                Duration.ofMinutes(30));
        this.geradorLembretes = new GeradorLembretes(fonteMedicamentos, horariosMedicamento, fonteConsultas,
                lembreteRepository, historicoService, Duration.ofMinutes(60));
        AlertaService alertaService = new AlertaService(new ResponsaveisPorVinculo(usuarioRepository),
                usuarioRepository, notificador, lembreteRepository, historicoService);
        VerificadorLembretes verificador = new VerificadorLembretes(lembreteService, alertaService,
                usuarioRepository, notificador);
        this.agendador = new AgendadorLembretes(verificador::verificar, relogio, Duration.ofSeconds(30));
    }

    public void iniciar() {
        agendador.iniciar();
    }

    public void encerrar() {
        agendador.encerrar();
    }

    public MenuLembretes criarMenu(Scanner entrada, PrintStream saida) {
        return new MenuLembretes(entrada, saida, lembreteService, historicoService, geradorLembretes, relogio);
    }
}
