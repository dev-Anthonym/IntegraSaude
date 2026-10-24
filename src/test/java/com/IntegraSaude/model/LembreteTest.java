package com.IntegraSaude.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class LembreteTest {

    @Test
    void deveNascerComStatusPendente() {
        Lembrete lembrete = new Lembrete(
                1L, 10L, TipoLembrete.MEDICAMENTO, 5L,
                LocalDateTime.of(2026, 10, 24, 8, 0));

        assertEquals(StatusLembrete.PENDENTE, lembrete.getStatus());
    }
}