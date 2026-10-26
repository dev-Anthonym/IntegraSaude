package com.IntegraSaude.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import org.junit.jupiter.api.Test;

/** RN09: máquina de estados do lembrete. */
class LembreteTest {

    private Lembrete novo() {
        return new Lembrete(1L, TipoLembrete.MEDICAMENTO, 1L, CenarioTeste.AGORA, "Tomar remedio");
    }

    @Test
    void deveNascerComStatusPendente() {
        assertEquals(StatusLembrete.PENDENTE, novo().getStatus());
    }

    @Test
    void deveConfirmarLembretePendente() {
        Lembrete l = novo();
        l.confirmar();
        assertEquals(StatusLembrete.CONFIRMADO, l.getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoDeLembretePerdido() {
        Lembrete l = novo();
        l.marcarComoPerdido();
        assertThrows(OperacaoInvalidaException.class, l::confirmar);
    }

    @Test
    void deveManterEstadosFinais() {
        Lembrete l = novo();
        l.confirmar();
        assertThrows(OperacaoInvalidaException.class, l::marcarComoPerdido);
        assertThrows(OperacaoInvalidaException.class, l::confirmar);
    }
}
