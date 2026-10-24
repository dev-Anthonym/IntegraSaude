import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.IntegraSaude.exception.TransicaoInvalidaException;
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
}