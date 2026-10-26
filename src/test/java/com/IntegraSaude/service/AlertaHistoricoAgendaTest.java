package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.ItemAgenda;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN11, RN13 e RN14: alerta ao responsável, histórico e agenda. */
class AlertaHistoricoAgendaTest {

    private static final LocalDate HOJE = CenarioTeste.AGORA.toLocalDate();

    private final CenarioTeste c = new CenarioTeste(new ConfiguracaoLembretes(30, 60));

    // Gera um lembrete e o deixa vencer
    private Lembrete lembretePerdido(Usuario paciente) {
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(12, 0)));
        Lembrete l = c.gerador.gerarDoDia(paciente.getId(), HOJE).get(0);
        c.lembretes.verificarPerdidos(l.getHorarioPrevisto().plusMinutes(31));
        return l;
    }

    @Test
    void deveAlertarTodosOsResponsaveisDoPaciente() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r1@email.com").getId());
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r2@email.com").getId());
        assertEquals(2, c.alertas.alertarResponsaveis(lembretePerdido(paciente)));
        assertEquals(2, c.notificador.mensagens.size());
    }

    @Test
    void naoDeveAlertarQuandoNaoHaVinculo() {
        Usuario paciente = c.criarPaciente("p@email.com");
        assertEquals(0, c.alertas.alertarResponsaveis(lembretePerdido(paciente)));
        assertTrue(c.notificador.mensagens.isEmpty());
    }

    @Test
    void naoDeveAlertarDuasVezesPeloMesmoLembrete() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r@email.com").getId());
        Lembrete l = lembretePerdido(paciente);
        c.alertas.alertarResponsaveis(l);
        assertEquals(0, c.alertas.alertarResponsaveis(l));
        assertEquals(1, c.notificador.mensagens.size());
    }

    @Test
    void deveContinuarNotificandoSeUmEnvioFalhar() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Usuario r1 = c.criarResponsavel("r1@email.com");
        Usuario r2 = c.criarResponsavel("r2@email.com");
        c.vinculos.criar(paciente.getId(), r1.getId());
        c.vinculos.criar(paciente.getId(), r2.getId());
        c.notificador.destinatariosComFalha.add(r1.getId());
        assertEquals(1, c.alertas.alertarResponsaveis(lembretePerdido(paciente)));
        assertEquals(1, c.notificador.mensagens.size());
    }

    @Test
    void deveRegistrarAlertaNoHistorico() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r@email.com").getId());
        Lembrete l = lembretePerdido(paciente);
        c.alertas.alertarResponsaveis(l);
        assertTrue(c.historico.jaRegistrado(l.getId(), TipoEvento.ALERTA_ENVIADO));
    }

    @Test
    void deveListarHistoricoOrdenado() {
        Usuario paciente = c.criarPaciente("p@email.com");
        lembretePerdido(paciente);
        List<EventoHistorico> eventos = c.historico.listarDoPaciente(paciente.getId());
        assertEquals(TipoEvento.GERADO, eventos.get(0).getTipo());
        assertEquals(TipoEvento.PERDIDO, eventos.get(1).getTipo());
    }

    @Test
    void deveFiltrarHistoricoPorPeriodo() {
        Usuario paciente = c.criarPaciente("p@email.com");
        lembretePerdido(paciente);
        assertEquals(2, c.historico.listarPorPeriodo(paciente.getId(),
                CenarioTeste.AGORA.minusHours(1), CenarioTeste.AGORA.plusHours(1)).size());
        assertTrue(c.historico.listarPorPeriodo(paciente.getId(),
                CenarioTeste.AGORA.plusDays(1), CenarioTeste.AGORA.plusDays(2)).isEmpty());
    }

    @Test
    void deveMostrarMedicamentosDoDiaOrdenados() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 8, LocalTime.of(6, 0)));
        List<ItemAgenda> itens = c.agenda.agendaDoDia(paciente.getId(), HOJE);
        assertEquals(3, itens.size());
        assertEquals(HOJE.atTime(6, 0), itens.get(0).dataHora());
        assertEquals(HOJE.atTime(22, 0), itens.get(2).dataHora());
    }

    @Test
    void deveMisturarMedicamentosEConsultasOrdenadosPorHora() {
        Usuario paciente = c.criarPaciente("p@email.com");
        // Remédio às 15:00 e consulta às 14:00: a consulta deve vir primeiro
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(15, 0)));
        c.consultas.cadastrar(c.consulta(paciente.getId(), c.criarMedico(), HOJE.atTime(14, 0)));
        List<ItemAgenda> itens = c.agenda.agendaDoDia(paciente.getId(), HOJE);
        assertEquals(TipoLembrete.CONSULTA, itens.get(0).tipo());
        assertEquals(TipoLembrete.MEDICAMENTO, itens.get(1).tipo());
    }

    @Test
    void deveRetornarAgendaVaziaQuandoNaoHaItens() {
        Usuario paciente = c.criarPaciente("p@email.com");
        assertTrue(c.agenda.agendaDoDia(paciente.getId(), HOJE).isEmpty());
    }

    @Test
    void deveCobrirSeteDiasNaAgendaSemanal() {
        Usuario paciente = c.criarPaciente("p@email.com");
        // Remédio com 8 dias de período e 1 dose por dia: a semana mostra exatamente 7
        c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 24, LocalTime.of(8, 0)));
        assertEquals(7, c.agenda.agendaDaSemana(paciente.getId(), HOJE).size());
    }
}
