package com.IntegraSaude.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

// Executa a verificação de lembretes em segundo plano a cada intervalo
public class AgendadorLembretes {

    private static final Logger LOGGER = Logger.getLogger(AgendadorLembretes.class.getName());
    private static final long SEGUNDOS_PARA_ENCERRAR = 5;

    private final Consumer<LocalDateTime> verificacao;
    private final Clock relogio;
    private final Duration intervalo;
    private ScheduledExecutorService executor;

    public AgendadorLembretes(Consumer<LocalDateTime> verificacao, Clock relogio, Duration intervalo) {
        this.verificacao = verificacao;
        this.relogio = relogio;
        this.intervalo = intervalo;
    }

    public synchronized void iniciar() {
        if (estaAtivo()) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(this::criarThread);
        executor.scheduleAtFixedRate(this::executarVerificacao, 0, intervalo.toMillis(), TimeUnit.MILLISECONDS);
    }

    // Uma exceção não tratada cancelaria as próximas execuções do ScheduledExecutorService
    void executarVerificacao() {
        try {
            verificacao.accept(LocalDateTime.now(relogio));
        } catch (RuntimeException e) {
            LOGGER.warning("Falha na verificação de lembretes: " + e.getMessage());
            LOGGER.log(Level.FINE, "Detalhes da falha na verificação", e);
        }
    }

    public synchronized void encerrar() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SEGUNDOS_PARA_ENCERRAR, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public synchronized boolean estaAtivo() {
        return executor != null && !executor.isShutdown();
    }

    // Thread daemon para que a aplicação nunca fique presa ao sair
    private Thread criarThread(Runnable tarefa) {
        Thread thread = new Thread(tarefa, "agendador-lembretes");
        thread.setDaemon(true);
        return thread;
    }
}
