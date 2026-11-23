package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.scheduler.VerificadorLembretes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** RN11: o responsável é alertado uma única vez por lembrete perdido, mesmo com falhas e várias rodadas. */
class AlertaUnicoTest {

    private static final LocalDateTime DEPOIS_DO_PRAZO = CenarioTeste.AGORA.plusMinutes(31);

    private CenarioTeste cenario;
    private Usuario paciente;
    private Usuario joao;
    private Lembrete lembrete;

    @BeforeEach
    void preparar() {
        cenario = new CenarioTeste();
        paciente = cenario.criarPaciente("maria@teste.com");
        joao = cenario.criarResponsavel("joao@teste.com");
        cenario.vinculos.criar(paciente.getId(), joao.getId());
        cenario.medicamentos.cadastrar(cenario.medicamento(paciente.getId(), 24, LocalTime.of(10, 0)));
        lembrete = cenario.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).getFirst();
    }

    private long eventosDeAlerta() {
        return cenario.historico.listarDoPaciente(paciente.getId()).stream()
                .filter(e -> e.getTipo() == TipoEvento.ALERTA_ENVIADO).count();
    }

    @Test
    void naoDeveAlertarLembretePendenteNemConfirmado() {
        assertThrows(OperacaoInvalidaException.class, () -> cenario.alertas.alertarResponsaveis(lembrete));
        cenario.lembretes.confirmar(paciente.getId(), lembrete.getId());
        assertThrows(OperacaoInvalidaException.class, () -> cenario.alertas.alertarResponsaveis(lembrete));
        assertTrue(cenario.notificador.mensagens.isEmpty());
    }

    @Test
    void mensagemDeveIdentificarOPacienteEOLembrete() {
        cenario.lembretes.verificarPerdidos(DEPOIS_DO_PRAZO);
        assertEquals(1, cenario.alertas.alertarResponsaveis(lembrete));
        String mensagem = cenario.notificador.mensagens.getFirst();
        assertTrue(mensagem.startsWith(joao.getNome() + ": ALERTA: " + paciente.getNome()), mensagem);
        assertTrue(mensagem.contains(lembrete.getDescricao()));
    }

    @Test
    void naoDeveMarcarAlertaQuandoTodosOsEnviosFalham() {
        cenario.notificador.destinatariosComFalha.add(joao.getId());
        cenario.lembretes.verificarPerdidos(DEPOIS_DO_PRAZO);

        assertEquals(0, cenario.alertas.alertarResponsaveis(lembrete));
        assertFalse(lembrete.isAlertaEnviado(), "sem entrega, o alerta deve poder ser tentado de novo");
        assertEquals(0, eventosDeAlerta());

        // Quando o canal volta, a nova tentativa entrega e marca uma única vez
        cenario.notificador.destinatariosComFalha.clear();
        assertEquals(1, cenario.alertas.alertarResponsaveis(lembrete));
        assertTrue(lembrete.isAlertaEnviado());
        assertEquals(0, cenario.alertas.alertarResponsaveis(lembrete));
        assertEquals(1, eventosDeAlerta());
    }

    @Test
    void naoDeveAlertarResponsavelDesvinculado() {
        cenario.vinculos.remover(paciente.getId(), joao.getId());
        cenario.lembretes.verificarPerdidos(DEPOIS_DO_PRAZO);
        assertEquals(0, cenario.alertas.alertarResponsaveis(lembrete));
        assertTrue(cenario.notificador.mensagens.isEmpty());
    }

    @Test
    void naoDeveAlertarResponsavelDeOutroPaciente() {
        Usuario pedro = cenario.criarPaciente("pedro@teste.com");
        Usuario ana = cenario.criarResponsavel("ana@teste.com");
        cenario.vinculos.criar(pedro.getId(), ana.getId());
        cenario.lembretes.verificarPerdidos(DEPOIS_DO_PRAZO);
        cenario.alertas.alertarResponsaveis(lembrete);
        assertTrue(cenario.notificador.mensagens.stream().noneMatch(m -> m.startsWith(ana.getNome())));
    }

    @Test
    void verificadorEmVariasRodadasDeveAlertarUmaUnicaVez() {
        VerificadorLembretes verificador = new VerificadorLembretes(cenario.usuarios, cenario.gerador,
                cenario.lembretes, cenario.alertas, cenario.notificador);
        verificador.verificar(DEPOIS_DO_PRAZO);
        verificador.verificar(DEPOIS_DO_PRAZO.plusMinutes(1));
        verificador.verificar(DEPOIS_DO_PRAZO.plusHours(1));

        long alertas = cenario.notificador.mensagens.stream().filter(m -> m.contains("ALERTA")).count();
        assertEquals(1, alertas);
        assertEquals(1, eventosDeAlerta());
    }

    @Test
    void verificadorDeveAvisarOPacienteUmaVezQuandoChegaAHora() {
        VerificadorLembretes verificador = new VerificadorLembretes(cenario.usuarios, cenario.gerador,
                cenario.lembretes, cenario.alertas, cenario.notificador);
        verificador.verificar(CenarioTeste.AGORA);
        verificador.verificar(CenarioTeste.AGORA.plusMinutes(5));
        long avisos = cenario.notificador.mensagens.stream().filter(m -> m.contains("E hora:")).count();
        assertEquals(1, avisos);
        assertTrue(cenario.notificador.mensagens.getFirst().contains("codigo " + lembrete.getId()));
    }
}
