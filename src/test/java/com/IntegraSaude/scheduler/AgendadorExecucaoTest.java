package com.IntegraSaude.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.RelogioAjustavel;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** O agendador roda de verdade em segundo plano, usa o relógio injetado e para quando pedido. */
class AgendadorExecucaoTest {

    @Test
    @Timeout(10)
    void deveExecutarImediatamenteComOAgoraDoRelogioEPararDepois() throws InterruptedException {
        List<LocalDateTime> execucoes = new CopyOnWriteArrayList<>();
        CountDownLatch primeira = new CountDownLatch(1);
        AgendadorLembretes agendador = new AgendadorLembretes(agora -> {
            execucoes.add(agora);
            primeira.countDown();
        }, new RelogioAjustavel(CenarioTeste.AGORA), 60);

        agendador.iniciar();
        assertTrue(primeira.await(5, TimeUnit.SECONDS), "a primeira rodada e imediata");
        agendador.parar();

        assertEquals(List.of(CenarioTeste.AGORA), execucoes, "intervalo de 60s: so a rodada inicial");
    }

    @Test
    @Timeout(10)
    void deveSeguirRodandoMesmoComFalhasERespeitarInterrupcao() throws InterruptedException {
        CountDownLatch tresRodadas = new CountDownLatch(3);
        AgendadorLembretes agendador = new AgendadorLembretes(agora -> {
            tresRodadas.countDown();
            throw new IllegalStateException("falha simulada");
        }, new RelogioAjustavel(CenarioTeste.AGORA), 1);

        agendador.iniciar();
        assertTrue(tresRodadas.await(8, TimeUnit.SECONDS), "uma excecao nao pode cancelar o agendamento");

        // parar() chamado por uma thread interrompida deve encerrar e manter a interrupção
        Thread.currentThread().interrupt();
        agendador.parar();
        assertTrue(Thread.interrupted());
    }
}
