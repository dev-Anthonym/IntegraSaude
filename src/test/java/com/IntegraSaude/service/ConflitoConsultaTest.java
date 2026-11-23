package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.StatusConsulta;
import com.IntegraSaude.model.Usuario;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Conflito de horário, edição e cancelamento de consultas (RN06 e RN08). */
class ConflitoConsultaTest {

    private static final LocalDateTime AMANHA_9H = CenarioTeste.AGORA.plusDays(1).withHour(9);

    private CenarioTeste cenario;
    private Usuario paciente;
    private Medico medico;

    @BeforeEach
    void preparar() {
        cenario = new CenarioTeste();
        paciente = cenario.criarPaciente("maria@teste.com");
        medico = cenario.criarMedico();
    }

    private Consulta agendar(LocalDateTime dataHora) {
        return cenario.consultas.cadastrar(cenario.consulta(paciente.getId(), medico, dataHora));
    }

    // Consulta nova com o mesmo id de uma existente (é assim que a edição funciona)
    private Consulta edicao(Consulta original, LocalDateTime novoHorario) {
        Consulta nova = cenario.consulta(paciente.getId(), medico, novoHorario);
        nova.setId(original.getId());
        return nova;
    }

    @Test
    void devePermitirConsultasUmMinutoAntesEDepois() {
        agendar(AMANHA_9H);
        assertDoesNotThrow(() -> agendar(AMANHA_9H.minusMinutes(1)));
        assertDoesNotThrow(() -> agendar(AMANHA_9H.plusMinutes(1)));
        assertEquals(3, cenario.consultas.listarDoPaciente(paciente.getId()).size());
    }

    @Test
    void deveRejeitarConflitoMesmoComOutroMedico() {
        agendar(AMANHA_9H);
        Medico outro = cenario.medicos.cadastrar("Dra. Ana", cenario.especialidades.cadastrar("Dermatologia").getId());
        assertThrows(RegistroDuplicadoException.class,
                () -> cenario.consultas.cadastrar(cenario.consulta(paciente.getId(), outro, AMANHA_9H)));
    }

    @Test
    void cancelarDeveLiberarOHorario() {
        Consulta primeira = agendar(AMANHA_9H);
        cenario.consultas.cancelar(paciente.getId(), primeira.getId());
        Consulta segunda = agendar(AMANHA_9H);
        assertEquals(StatusConsulta.CANCELADA, primeira.getStatus());
        assertEquals(1, cenario.consultas.listarDoPaciente(paciente.getId()).size());
        assertEquals(2, cenario.consultas.listarTodasDoPaciente(paciente.getId()).size());
        assertTrue(segunda.estaAgendada());
    }

    @Test
    void edicaoNoMesmoHorarioDaPropriaConsultaNaoEConflito() {
        Consulta consulta = agendar(AMANHA_9H);
        assertDoesNotThrow(() -> cenario.consultas.atualizar(edicao(consulta, AMANHA_9H)));
    }

    @Test
    void edicaoParaOHorarioDeOutraConsultaEConflito() {
        Consulta das9 = agendar(AMANHA_9H);
        agendar(AMANHA_9H.plusHours(2));
        assertThrows(RegistroDuplicadoException.class,
                () -> cenario.consultas.atualizar(edicao(das9, AMANHA_9H.plusHours(2))));
    }

    @Test
    void edicaoDeveDescartarLembretesPendentesDaConsulta() {
        // Consulta hoje às 11:00 gera lembrete às 10:00 (60 min de antecedência)
        Consulta consulta = agendar(CenarioTeste.AGORA.plusHours(1));
        assertEquals(1, cenario.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).size());
        cenario.consultas.atualizar(edicao(consulta, AMANHA_9H));
        assertTrue(cenario.lembretes.listarPendentes(paciente.getId()).isEmpty());
    }

    @Test
    void naoDeveEditarConsultaCancelada() {
        Consulta consulta = agendar(AMANHA_9H);
        cenario.consultas.cancelar(paciente.getId(), consulta.getId());
        assertThrows(OperacaoInvalidaException.class,
                () -> cenario.consultas.atualizar(edicao(consulta, AMANHA_9H.plusHours(1))));
    }

    @Test
    void naoDeveCancelarDuasVezes() {
        Consulta consulta = agendar(AMANHA_9H);
        cenario.consultas.cancelar(paciente.getId(), consulta.getId());
        assertThrows(OperacaoInvalidaException.class,
                () -> cenario.consultas.cancelar(paciente.getId(), consulta.getId()));
    }

    @Test
    void naoDeveCancelarConsultaQueJaPassou() {
        Consulta consulta = agendar(CenarioTeste.AGORA.plusMinutes(10));
        cenario.relogio.avancar(Duration.ofMinutes(11));
        assertThrows(OperacaoInvalidaException.class,
                () -> cenario.consultas.cancelar(paciente.getId(), consulta.getId()));
        assertTrue(consulta.estaAgendada());
    }

    @Test
    void naoDeveMexerEmConsultaDeOutroPaciente() {
        Consulta consulta = agendar(AMANHA_9H);
        Usuario pedro = cenario.criarPaciente("pedro@teste.com");
        assertThrows(RegistroNaoEncontradoException.class,
                () -> cenario.consultas.cancelar(pedro.getId(), consulta.getId()));
        Consulta invasora = cenario.consulta(pedro.getId(), medico, AMANHA_9H.plusHours(1));
        invasora.setId(consulta.getId());
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.consultas.atualizar(invasora));
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.consultas.cancelar(paciente.getId(), 99L));
    }

    @Test
    void consultaDeveTerDonoPaciente() {
        Usuario joao = cenario.criarResponsavel("joao@teste.com");
        assertThrows(DadoInvalidoException.class,
                () -> cenario.consultas.cadastrar(cenario.consulta(joao.getId(), medico, AMANHA_9H)));
        assertThrows(DadoInvalidoException.class,
                () -> cenario.consultas.cadastrar(cenario.consulta(null, medico, AMANHA_9H)));
        assertThrows(DadoInvalidoException.class,
                () -> cenario.consultas.cadastrar(cenario.consulta(999L, medico, AMANHA_9H)));
    }

    @Test
    void validadorDeveExigirMedicoLocalEDataHora() {
        ValidadorConsulta validador = new ValidadorConsulta(cenario.relogio);
        Long id = paciente.getId();
        assertThrows(DadoInvalidoException.class, () -> validador.validar(new Consulta(id, null, "Local", AMANHA_9H)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Consulta(id, new Medico("Sem especialidade", null), "Local", AMANHA_9H)));
        assertThrows(DadoInvalidoException.class, () -> validador.validar(new Consulta(id, medico, null, AMANHA_9H)));
        assertThrows(DadoInvalidoException.class, () -> validador.validar(new Consulta(id, medico, "   ", AMANHA_9H)));
        assertThrows(DadoInvalidoException.class, () -> validador.validar(new Consulta(id, medico, "Local", null)));
    }

    @Test
    void consultaExatamenteAgoraNaoEstaNoPassado() {
        ValidadorConsulta validador = new ValidadorConsulta(cenario.relogio);
        assertDoesNotThrow(() -> validador.validar(cenario.consulta(paciente.getId(), medico, CenarioTeste.AGORA)));
        assertThrows(DadoInvalidoException.class, () -> validador.validar(
                cenario.consulta(paciente.getId(), medico, CenarioTeste.AGORA.minusMinutes(1))));
    }

    @Test
    void consultaCanceladaNaoGeraLembrete() {
        Consulta consulta = agendar(CenarioTeste.AGORA.plusHours(1));
        cenario.consultas.cancelar(paciente.getId(), consulta.getId());
        assertTrue(cenario.gerador.gerarDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).isEmpty());
        assertFalse(cenario.agenda.agendaDoDia(paciente.getId(), CenarioTeste.AGORA.toLocalDate()).stream()
                .anyMatch(i -> i.itemId().equals(consulta.getId())));
    }

    @Test
    void especialidadeEMedicoDevemValidarCadastro() {
        assertThrows(DadoInvalidoException.class, () -> cenario.especialidades.cadastrar(" "));
        assertThrows(DadoInvalidoException.class, () -> cenario.especialidades.cadastrar(null));
        assertThrows(RegistroDuplicadoException.class, () -> cenario.especialidades.cadastrar(" cardiologia "));
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.especialidades.buscarPorId(99L));

        Especialidade cardio = cenario.especialidades.listar().getFirst();
        assertThrows(DadoInvalidoException.class, () -> cenario.medicos.cadastrar("", cardio.getId()));
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.medicos.cadastrar("Dr. X", 99L));
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.medicos.buscarPorId(99L));
        assertEquals("Dr. Y", cenario.medicos.cadastrar("  Dr. Y ", cardio.getId()).getNome());
        assertEquals(2, cenario.medicos.listarPorEspecialidade(cardio.getId()).size());
    }
}
