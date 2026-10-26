package com.IntegraSaude.scheduler;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Executa a verificação de lembretes em segundo plano, a cada intervalo fixo. */
public class AgendadorLembretes {

    private static final Logger LOGGER = Logger.getLogger(AgendadorLembretes.class.getName());
    private static final int SEGUNDOS_ESPERA_ENCERRAMENTO = 5;

    private final Consumer<LocalDateTime> tarefa;
    private final Clock relogio;
    private final int intervaloSegundos;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(corpo -> {
        // Thread daemon: se alguém esquecer de chamar parar(), o programa ainda encerra
        Thread thread = new Thread(corpo, "agendador-lembretes");
        thread.setDaemon(true);
        return thread;
    });

    // "tarefa" normalmente é verificador::verificar
    public AgendadorLembretes(Consumer<LocalDateTime> tarefa, Clock relogio, int intervaloSegundos) {
        this.tarefa = tarefa;
        this.relogio = relogio;
        this.intervaloSegundos = intervaloSegundos;
    }

    // Começa a verificar agora e depois a cada intervalo
    public void iniciar() {
        executor.scheduleAtFixedRate(this::executarCiclo, 0, intervaloSegundos, TimeUnit.SECONDS);
    }

    // Uma rodada. O try/catch é obrigatório: uma exceção solta cancelaria as próximas execuções em silêncio
    public void executarCiclo() {
        try {
            tarefa.accept(LocalDateTime.now(relogio));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Falha na verificacao de lembretes", e);
        }
    }

    // Encerra o agendador para o programa terminar sem ficar preso
    public void parar() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SEGUNDOS_ESPERA_ENCERRAMENTO, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
