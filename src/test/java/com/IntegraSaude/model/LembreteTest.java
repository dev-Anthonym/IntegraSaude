package com.IntegraSaude.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.exception.TransicaoInvalidaException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class LembreteTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Duration LIMITE = Duration.ofMinutes(30);

    @Test
    void deveNascerComStatusPendente() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));

        assertEquals(StatusLembrete.PENDENTE, lembrete.getStatus());
    }

    @Test
    void deveConfirmarLembretePendente() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));

        lembrete.confirmar();

        assertEquals(StatusLembrete.CONFIRMADO, lembrete.getStatus());
    }

    @Test
    void deveMarcarComoPerdidoLembretePendente() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));

        lembrete.marcarComoPerdido();

        assertEquals(StatusLembrete.PERDIDO, lembrete.getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoDeLembreteJaConfirmado() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));
        lembrete.confirmar();

        assertThrows(TransicaoInvalidaException.class, lembrete::confirmar);
    }

    @Test
    void deveRejeitarConfirmacaoDeLembretePerdido() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));
        lembrete.marcarComoPerdido();

        assertThrows(TransicaoInvalidaException.class, lembrete::confirmar);
    }

    @Test
    void deveRejeitarMarcarComoPerdidoLembreteConfirmado() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));
        lembrete.confirmar();

        assertThrows(TransicaoInvalidaException.class, lembrete::marcarComoPerdido);
    }

    @Test
    void deveEstarVencidoAposOTempoLimite() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));

        Clock relogio = relogioEm(LocalDateTime.of(2026, 10, 24, 8, 31));

        assertTrue(lembrete.estaVencido(relogio, LIMITE));
    }

    @Test
    void naoDeveEstarVencidoAntesDoTempoLimite() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));

        Clock relogio = relogioEm(LocalDateTime.of(2026, 10, 24, 8, 29));

        assertFalse(lembrete.estaVencido(relogio, LIMITE));
    }

    private Clock relogioEm(LocalDateTime momento) {
        return Clock.fixed(momento.atZone(FUSO).toInstant(), FUSO);
    }
}