package com.IntegraSaude.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Etapa 13: lógica da verificação e resistência a falhas do agendador. */
class AgendadorTest {

    private final CenarioTeste c = new CenarioTeste(new ConfiguracaoLembretes(30, 60));
    private final VerificadorLembretes verificador = new VerificadorLembretes(
            c.usuarios, c.gerador, c.lembretes, c.alertas, c.notificador);

    // Paciente com remédio às 08:00 e um responsável vinculado
    private Usuario cenarioComResponsavel() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r@email.com").getId());
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(8, 0)));
        return paciente;
    }

    @Test
    void deveVerificarLembretesVencidosNoMomentoInformado() {
        cenarioComResponsavel();
        var oitoEDez = CenarioTeste.AGORA.toLocalDate().atTime(8, 10);
        verificador.verificar(oitoEDez);
        Lembrete l = c.repos.lembretes().buscarPorId(1L).orElseThrow();
        assertEquals(StatusLembrete.PENDENTE, l.getStatus());
        // Aviso "e hora" ao paciente, uma única vez
        verificador.verificar(oitoEDez.plusMinutes(5));
        assertEquals(1, c.notificador.mensagens.size());
        // 08:45 passou do prazo de 30 minutos
        verificador.verificar(oitoEDez.plusMinutes(35));
        assertEquals(StatusLembrete.PERDIDO, l.getStatus());
    }

    @Test
    void deveDispararAlertaParaLembreteVencido() {
        cenarioComResponsavel();
        var oitoEDez = CenarioTeste.AGORA.toLocalDate().atTime(8, 10);
        verificador.verificar(oitoEDez);
        verificador.verificar(oitoEDez.plusMinutes(35));
        assertTrue(c.notificador.mensagens.stream().anyMatch(m -> m.contains("ALERTA")));
    }

    @Test
    void deveContinuarAgendadoQuandoUmaVerificacaoFalha() {
        AtomicInteger chamadas = new AtomicInteger();
        AgendadorLembretes agendador = new AgendadorLembretes(agora -> {
            if (chamadas.incrementAndGet() == 1) {
                throw new IllegalStateException("falha simulada");
            }
        }, c.relogio, 30);
        agendador.executarCiclo();
        agendador.executarCiclo();
        assertEquals(2, chamadas.get());
        agendador.parar();
    }
}
