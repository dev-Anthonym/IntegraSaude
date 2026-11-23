package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.Usuario;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Valores limite do prazo de confirmação (RN10) e da detecção de lembrete perdido. */
class PrazoLembreteTest {

    private static final LocalDateTime DEZ_HORAS = CenarioTeste.AGORA;

    private CenarioTeste cenario;
    private Usuario paciente;
    private Lembrete lembrete;

    // Remédio diário às 10:00: o lembrete das 10:00 vence às 10:30
    @BeforeEach
    void preparar() {
        cenario = new CenarioTeste();
        paciente = cenario.criarPaciente("maria@teste.com");
        cenario.medicamentos.cadastrar(cenario.medicamento(paciente.getId(), 24, LocalTime.of(10, 0)));
        lembrete = cenario.gerador.gerarDoDia(paciente.getId(), DEZ_HORAS.toLocalDate()).getFirst();
    }

    @Test
    void deveAceitarConfirmacaoExatamenteNoLimiteDe30Minutos() {
        cenario.relogio.avancar(Duration.ofMinutes(30));
        Lembrete confirmado = cenario.lembretes.confirmar(paciente.getId(), lembrete.getId());
        assertEquals(StatusLembrete.CONFIRMADO, confirmado.getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoUmSegundoDepoisDoLimite() {
        cenario.relogio.avancar(Duration.ofMinutes(30).plusSeconds(1));
        assertThrows(OperacaoInvalidaException.class,
                () -> cenario.lembretes.confirmar(paciente.getId(), lembrete.getId()));
        assertTrue(lembrete.estaPendente(), "rejeitar nao pode mudar o status");
    }

    @Test
    void naoDeveMarcarPerdidoExatamenteNoLimite() {
        assertTrue(cenario.lembretes.verificarPerdidos(DEZ_HORAS.plusMinutes(30)).isEmpty());
        assertTrue(lembrete.estaPendente());
    }

    @Test
    void deveMarcarPerdidoUmSegundoDepoisDoLimite() {
        List<Lembrete> perdidos = cenario.lembretes.verificarPerdidos(DEZ_HORAS.plusMinutes(30).plusSeconds(1));
        assertEquals(List.of(lembrete), perdidos);
        assertEquals(StatusLembrete.PERDIDO, lembrete.getStatus());
        assertTrue(cenario.historico.jaRegistrado(lembrete.getId(), TipoEvento.PERDIDO));
    }

    @Test
    void naoDeveMarcarOMesmoLembreteComoPerdidoDuasVezes() {
        LocalDateTime depois = DEZ_HORAS.plusMinutes(31);
        assertEquals(1, cenario.lembretes.verificarPerdidos(depois).size());
        assertTrue(cenario.lembretes.verificarPerdidos(depois.plusHours(1)).isEmpty());
        long eventosPerdido = cenario.historico.listarDoPaciente(paciente.getId()).stream()
                .filter(e -> e.getTipo() == TipoEvento.PERDIDO).count();
        assertEquals(1, eventosPerdido);
    }

    @Test
    void naoDeveConfirmarLembreteJaConfirmado() {
        cenario.lembretes.confirmar(paciente.getId(), lembrete.getId());
        assertThrows(OperacaoInvalidaException.class,
                () -> cenario.lembretes.confirmar(paciente.getId(), lembrete.getId()));
    }

    @Test
    void lembreteConfirmadoNaoViraPerdido() {
        cenario.lembretes.confirmar(paciente.getId(), lembrete.getId());
        assertTrue(cenario.lembretes.verificarPerdidos(DEZ_HORAS.plusHours(2)).isEmpty());
        assertEquals(StatusLembrete.CONFIRMADO, lembrete.getStatus());
    }

    @Test
    void naoDeveConfirmarLembreteDeOutroPaciente() {
        Usuario outro = cenario.criarPaciente("pedro@teste.com");
        assertThrows(RegistroNaoEncontradoException.class,
                () -> cenario.lembretes.confirmar(outro.getId(), lembrete.getId()));
        assertTrue(lembrete.estaPendente());
    }

    @Test
    void deveFalharAoConfirmarLembreteInexistente() {
        assertThrows(RegistroNaoEncontradoException.class,
                () -> cenario.lembretes.confirmar(paciente.getId(), 999L));
    }

    @Test
    void deveRespeitarTempoLimiteConfigurado() {
        CenarioTeste demo = new CenarioTeste(new ConfiguracaoLembretes(1, 60));
        Usuario maria = demo.criarPaciente("maria@demo.com");
        demo.medicamentos.cadastrar(demo.medicamento(maria.getId(), 24, LocalTime.of(10, 0)));
        demo.gerador.gerarDoDia(maria.getId(), DEZ_HORAS.toLocalDate());
        assertTrue(demo.lembretes.verificarPerdidos(DEZ_HORAS.plusMinutes(1)).isEmpty());
        assertEquals(1, demo.lembretes.verificarPerdidos(DEZ_HORAS.plusMinutes(1).plusSeconds(1)).size());
    }

    @Test
    void geradorDeveCriarLembreteAindaDentroDoPrazoEIgnorarOVencido() {
        CenarioTeste outro = new CenarioTeste();
        Usuario maria = outro.criarPaciente("maria@teste.com");
        outro.medicamentos.cadastrar(outro.medicamento(maria.getId(), 24, LocalTime.of(9, 30)));
        // 09:30 + 30 min = 10:00 = agora: ainda dentro do prazo
        assertEquals(1, outro.gerador.gerarDoDia(maria.getId(), DEZ_HORAS.toLocalDate()).size());

        CenarioTeste vencido = new CenarioTeste();
        Usuario jose = vencido.criarPaciente("jose@teste.com");
        vencido.medicamentos.cadastrar(vencido.medicamento(jose.getId(), 24, LocalTime.of(9, 29)));
        assertTrue(vencido.gerador.gerarDoDia(jose.getId(), DEZ_HORAS.toLocalDate()).isEmpty());
    }

    @Test
    void deveListarDevidosSoQuandoOHorarioChega() {
        assertEquals(1, cenario.lembretes.listarDevidos(DEZ_HORAS).size());
        assertTrue(cenario.lembretes.listarDevidos(DEZ_HORAS.minusSeconds(1)).isEmpty());
    }

    @Test
    void deveListarPendentesOrdenadosPeloHorario() {
        CenarioTeste outro = new CenarioTeste();
        Usuario maria = outro.criarPaciente("maria@teste.com");
        outro.medicamentos.cadastrar(outro.medicamento(maria.getId(), 24, LocalTime.of(22, 0)));
        outro.medicamentos.cadastrar(outro.medicamento(maria.getId(), 24, LocalTime.of(12, 0)));
        outro.gerador.gerarDoDia(maria.getId(), DEZ_HORAS.toLocalDate());
        List<Lembrete> pendentes = outro.lembretes.listarPendentes(maria.getId());
        assertEquals(LocalTime.of(12, 0), pendentes.get(0).getHorarioPrevisto().toLocalTime());
        assertEquals(LocalTime.of(22, 0), pendentes.get(1).getHorarioPrevisto().toLocalTime());
    }

    @Test
    void configuracaoDeveRejeitarValoresInvalidos() {
        assertThrows(DadoInvalidoException.class, () -> new ConfiguracaoLembretes(0, 60));
        assertThrows(DadoInvalidoException.class, () -> new ConfiguracaoLembretes(-1, 60));
        assertThrows(DadoInvalidoException.class, () -> new ConfiguracaoLembretes(30, -1));
        assertEquals(0, new ConfiguracaoLembretes(30, 0).antecedenciaConsultaMinutos());
    }
}
