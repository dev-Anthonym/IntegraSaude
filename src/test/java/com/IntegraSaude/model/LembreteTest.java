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
    private static final LocalDateTime HORARIO_PREVISTO =
            LocalDateTime.of(2026, 10, 24, 8, 0);

    @Test
    void deveNascerComStatusPendente() {
        Lembrete lembrete = criarLembrete();

        assertEquals(StatusLembrete.PENDENTE, lembrete.getStatus());
    }

    @Test
    void deveConfirmarLembretePendente() {
        Lembrete lembrete = criarLembrete();

        lembrete.confirmar();

        assertEquals(StatusLembrete.CONFIRMADO, lembrete.getStatus());
    }

    @Test
    void deveMarcarComoPerdidoLembretePendente() {
        Lembrete lembrete = criarLembrete();

        lembrete.marcarComoPerdido();

        assertEquals(StatusLembrete.PERDIDO, lembrete.getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoDeLembreteJaConfirmado() {
        Lembrete lembrete = criarLembrete();
        lembrete.confirmar();

        assertThrows(TransicaoInvalidaException.class, lembrete::confirmar);
    }

    @Test
    void deveRejeitarConfirmacaoDeLembretePerdido() {
        Lembrete lembrete = criarLembrete();
        lembrete.marcarComoPerdido();

        assertThrows(TransicaoInvalidaException.class, lembrete::confirmar);
    }

    @Test
    void deveRejeitarMarcarComoPerdidoLembreteConfirmado() {
        Lembrete lembrete = criarLembrete();
        lembrete.confirmar();

        assertThrows(TransicaoInvalidaException.class, lembrete::marcarComoPerdido);
    }

    @Test
    void deveEstarVencidoAposOTempoLimite() {
        Lembrete lembrete = criarLembrete();
        Clock relogio = relogioEm(HORARIO_PREVISTO.plusMinutes(31));

        assertTrue(lembrete.estaVencido(relogio, LIMITE));
    }

    @Test
    void naoDeveEstarVencidoAntesDoTempoLimite() {
        Lembrete lembrete = criarLembrete();
        Clock relogio = relogioEm(HORARIO_PREVISTO.plusMinutes(29));

        assertFalse(lembrete.estaVencido(relogio, LIMITE));
    }

    private Lembrete criarLembrete() {
        return new Lembrete(1L, 10L, TipoLembrete.MEDICAMENTO, 5L, HORARIO_PREVISTO);
    }

    private Clock relogioEm(LocalDateTime momento) {
        return Clock.fixed(momento.atZone(FUSO).toInstant(), FUSO);
    }
}