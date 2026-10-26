package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.Usuario;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN06 e RN08: consultas. */
class ConsultaServiceTest {

    private final CenarioTeste c = new CenarioTeste();

    @Test
    void deveCadastrarConsultaParaPaciente() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Medico medico = c.criarMedico();
        c.consultas.cadastrar(c.consulta(paciente.getId(), medico, CenarioTeste.AGORA.plusDays(2)));
        assertEquals(1, c.consultas.listarDoPaciente(paciente.getId()).size());
    }

    @Test
    void deveRejeitarConsultaNoMesmoHorarioDoPaciente() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Medico medico = c.criarMedico();
        LocalDateTime horario = CenarioTeste.AGORA.plusDays(2);
        c.consultas.cadastrar(c.consulta(paciente.getId(), medico, horario));
        assertThrows(RegistroDuplicadoException.class,
                () -> c.consultas.cadastrar(c.consulta(paciente.getId(), medico, horario)));
    }

    @Test
    void devePermitirMesmoHorarioParaPacientesDiferentes() {
        Usuario p1 = c.criarPaciente("p1@email.com");
        Usuario p2 = c.criarPaciente("p2@email.com");
        Medico medico = c.criarMedico();
        LocalDateTime horario = CenarioTeste.AGORA.plusDays(2);
        c.consultas.cadastrar(c.consulta(p1.getId(), medico, horario));
        c.consultas.cadastrar(c.consulta(p2.getId(), medico, horario));
        assertEquals(1, c.consultas.listarDoPaciente(p2.getId()).size());
    }

    @Test
    void deveRejeitarEdicaoDeConsultaQueJaPassou() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Medico medico = c.criarMedico();
        Consulta salva = c.consultas.cadastrar(c.consulta(paciente.getId(), medico, CenarioTeste.AGORA.plusHours(2)));
        c.relogio.avancar(Duration.ofHours(3));
        Consulta nova = c.consulta(paciente.getId(), medico, CenarioTeste.AGORA.plusDays(5));
        nova.setId(salva.getId());
        assertThrows(OperacaoInvalidaException.class, () -> c.consultas.atualizar(nova));
    }

    @Test
    void deveListarConsultasOrdenadasPorData() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Medico medico = c.criarMedico();
        LocalDateTime tarde = CenarioTeste.AGORA.plusDays(5);
        LocalDateTime cedo = CenarioTeste.AGORA.plusDays(1);
        c.consultas.cadastrar(c.consulta(paciente.getId(), medico, tarde));
        c.consultas.cadastrar(c.consulta(paciente.getId(), medico, cedo));
        List<Consulta> lista = c.consultas.listarDoPaciente(paciente.getId());
        assertEquals(cedo, lista.get(0).getDataHora());
        assertEquals(tarde, lista.get(1).getDataHora());
    }

    @Test
    void deveCancelarConsultaFutura() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Consulta salva = c.consultas.cadastrar(
                c.consulta(paciente.getId(), c.criarMedico(), CenarioTeste.AGORA.plusDays(1)));
        c.consultas.cancelar(paciente.getId(), salva.getId());
        assertEquals(0, c.consultas.listarDoPaciente(paciente.getId()).size());
    }
}
