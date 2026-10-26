package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.Usuario;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN09, RN10, RN12 e RN13: lembretes, tempo limite e geração sem duplicados. */
class LembreteGeradorTest {

    private final CenarioTeste c = new CenarioTeste(new ConfiguracaoLembretes(30, 60));

    // Paciente com remédio às 12:00 a cada 24h (nenhum horário vencido às 10:00)
    private Usuario pacienteComRemedio() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(12, 0)));
        return paciente;
    }

    @Test
    void deveGerarUmLembretePorHorarioDeMedicamento() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 6, LocalTime.of(12, 0)));
        // 12:00 e 18:00 ainda não venceram às 10:00 (00:00 e 06:00 já passaram, e nem existem no dia)
        List<Lembrete> criados = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
        assertEquals(2, criados.size());
    }

    @Test
    void naoDeveGerarLembreteDuplicado() {
        Usuario paciente = pacienteComRemedio();
        c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
        List<Lembrete> segundaVez = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
        assertTrue(segundaVez.isEmpty());
        assertEquals(1, c.lembretes.listarPendentes(paciente.getId()).size());
    }

    @Test
    void deveGerarLembreteDeConsultaComAntecedencia() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.consultas.cadastrar(c.consulta(paciente.getId(), c.criarMedico(), CenarioTeste.AGORA.plusHours(5)));
        List<Lembrete> criados = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
        assertEquals(1, criados.size());
        assertEquals(CenarioTeste.AGORA.plusHours(4), criados.get(0).getHorarioPrevisto());
    }

    @Test
    void deveRegistrarGeracaoNoHistorico() {
        Usuario paciente = pacienteComRemedio();
        c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate());
        assertEquals(TipoEvento.GERADO, c.historico.listarDoPaciente(paciente.getId()).get(0).getTipo());
    }

    @Test
    void deveConfirmarLembretePendenteERegistrarEvento() {
        Usuario paciente = pacienteComRemedio();
        Lembrete l = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).get(0);
        c.lembretes.confirmar(paciente.getId(), l.getId());
        assertEquals(StatusLembrete.CONFIRMADO, l.getStatus());
        assertTrue(c.historico.jaRegistrado(l.getId(), TipoEvento.CONFIRMADO));
    }

    @Test
    void deveMarcarComoPerdidoAposOTempoLimite() {
        Usuario paciente = pacienteComRemedio();
        Lembrete l = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).get(0);
        // 12:31 > 12:00 + 30 min
        List<Lembrete> perdidos = c.lembretes.verificarPerdidos(l.getHorarioPrevisto().plusMinutes(31));
        assertEquals(1, perdidos.size());
        assertEquals(StatusLembrete.PERDIDO, l.getStatus());
        assertTrue(c.historico.jaRegistrado(l.getId(), TipoEvento.PERDIDO));
    }

    @Test
    void naoDeveMarcarComoPerdidoAntesDoTempoLimite() {
        Usuario paciente = pacienteComRemedio();
        Lembrete l = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).get(0);
        assertTrue(c.lembretes.verificarPerdidos(l.getHorarioPrevisto().plusMinutes(30)).isEmpty());
        assertEquals(StatusLembrete.PENDENTE, l.getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoDepoisDoPrazo() {
        Usuario paciente = pacienteComRemedio();
        Lembrete l = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).get(0);
        c.relogio.avancar(Duration.ofHours(3));
        assertThrows(OperacaoInvalidaException.class, () -> c.lembretes.confirmar(paciente.getId(), l.getId()));
    }

    @Test
    void deveRejeitarConfirmacaoDeLembretePerdido() {
        Usuario paciente = pacienteComRemedio();
        Lembrete l = c.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).get(0);
        c.lembretes.verificarPerdidos(l.getHorarioPrevisto().plusMinutes(31));
        assertThrows(OperacaoInvalidaException.class, () -> c.lembretes.confirmar(paciente.getId(), l.getId()));
    }
}
