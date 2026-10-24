package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AgendadorLembretesTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 10, 24, 8, 31);
    private static final Clock RELOGIO = Clock.fixed(AGORA.atZone(FUSO).toInstant(), FUSO);

    @Test
    void deveVerificarUsandoOHorarioDoRelogio() {
        List<LocalDateTime> verificacoes = new ArrayList<>();
        AgendadorLembretes agendador = new AgendadorLembretes(verificacoes::add, RELOGIO, Duration.ofSeconds(30));

        agendador.executarVerificacao();

        assertEquals(List.of(AGORA), verificacoes);
    }

    @Test
    void deveContinuarAgendadoMesmoQuandoUmaVerificacaoFalha() {
        AtomicInteger execucoes = new AtomicInteger();
        AgendadorLembretes agendador = new AgendadorLembretes(agora -> {
            if (execucoes.incrementAndGet() == 1) {
                throw new IllegalStateException("Falha simulada na verificação");
            }
        }, RELOGIO, Duration.ofSeconds(30));

        assertDoesNotThrow(agendador::executarVerificacao);
        agendador.executarVerificacao();

        assertEquals(2, execucoes.get());
    }

    @Test
    void deveExecutarNovamenteNoAgendamentoRealAposFalha() throws InterruptedException {
        CountDownLatch duasExecucoes = new CountDownLatch(2);
        AgendadorLembretes agendador = new AgendadorLembretes(agora -> {
            duasExecucoes.countDown();
            throw new IllegalStateException("Falha simulada na verificação");
        }, RELOGIO, Duration.ofMillis(10));

        agendador.iniciar();
        boolean executouDuasVezes = duasExecucoes.await(2, TimeUnit.SECONDS);
        agendador.encerrar();

        assertTrue(executouDuasVezes);
    }

    @Test
    void deveEncerrarSemFicarPreso() {
        AgendadorLembretes agendador = new AgendadorLembretes(agora -> { }, RELOGIO, Duration.ofMillis(10));
        agendador.iniciar();

        agendador.encerrar();

        assertFalse(agendador.estaAtivo());
    }
}
