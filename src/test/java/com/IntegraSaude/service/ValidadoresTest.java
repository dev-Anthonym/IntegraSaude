package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.RelogioAjustavel;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.Perfil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** RN01, RN02, RN05 e RN08: validações. */
class ValidadoresTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel(CenarioTeste.AGORA);
    private final ValidadorUsuario validadorUsuario = new ValidadorUsuario();
    private final ValidadorMedicamento validadorMedicamento = new ValidadorMedicamento(relogio);
    private final ValidadorConsulta validadorConsulta = new ValidadorConsulta(relogio);

    private Medicamento medicamento(int frequencia, LocalDate inicio, LocalDate fim) {
        return new Medicamento(1L, "Dipirona", "500 mg", frequencia, LocalTime.of(8, 0), inicio, fim);
    }

    private Consulta consulta(LocalDateTime dataHora) {
        Medico medico = new Medico("Dr. Teste", new Especialidade("Cardiologia"));
        return new Consulta(1L, medico, "Clinica", dataHora);
    }

    @Test
    void deveRejeitarEmailSemArroba() {
        assertThrows(DadoInvalidoException.class,
                () -> validadorUsuario.validar("Ana", "anaemail.com", "Senha123", Perfil.PACIENTE));
    }

    @Test
    void deveRejeitarSenhaCurta() {
        assertThrows(DadoInvalidoException.class,
                () -> validadorUsuario.validar("Ana", "ana@email.com", "Ab1", Perfil.PACIENTE));
    }

    @Test
    void deveRejeitarSenhaSemNumero() {
        assertThrows(DadoInvalidoException.class,
                () -> validadorUsuario.validar("Ana", "ana@email.com", "SomenteLetras", Perfil.PACIENTE));
    }

    @Test
    void deveRejeitarNomeVazioEPerfilNulo() {
        assertThrows(DadoInvalidoException.class,
                () -> validadorUsuario.validar(" ", "ana@email.com", "Senha123", Perfil.PACIENTE));
        assertThrows(DadoInvalidoException.class,
                () -> validadorUsuario.validar("Ana", "ana@email.com", "Senha123", null));
    }

    @Test
    void deveRejeitarFrequenciaZero() {
        LocalDate hoje = CenarioTeste.AGORA.toLocalDate();
        assertThrows(DadoInvalidoException.class,
                () -> validadorMedicamento.validar(medicamento(0, hoje, hoje.plusDays(3))));
    }

    @Test
    void deveRejeitarFrequenciaMaiorQue24() {
        LocalDate hoje = CenarioTeste.AGORA.toLocalDate();
        assertThrows(DadoInvalidoException.class,
                () -> validadorMedicamento.validar(medicamento(25, hoje, hoje.plusDays(3))));
    }

    @Test
    void deveRejeitarDataInicioPosteriorAoFim() {
        LocalDate hoje = CenarioTeste.AGORA.toLocalDate();
        assertThrows(DadoInvalidoException.class,
                () -> validadorMedicamento.validar(medicamento(8, hoje.plusDays(5), hoje.plusDays(2))));
    }

    @Test
    void deveRejeitarDataFimNoPassado() {
        LocalDate hoje = CenarioTeste.AGORA.toLocalDate();
        assertThrows(DadoInvalidoException.class,
                () -> validadorMedicamento.validar(medicamento(8, hoje.minusDays(5), hoje.minusDays(1))));
    }

    @Test
    void deveRejeitarConsultaNoPassado() {
        assertThrows(DadoInvalidoException.class,
                () -> validadorConsulta.validar(consulta(CenarioTeste.AGORA.minusMinutes(1))));
    }

    @Test
    void deveAceitarDadosValidos() {
        LocalDate hoje = CenarioTeste.AGORA.toLocalDate();
        assertDoesNotThrow(() -> validadorUsuario.validar("Ana", "ana@email.com", "Senha123", Perfil.PACIENTE));
        assertDoesNotThrow(() -> validadorMedicamento.validar(medicamento(8, hoje, hoje.plusDays(3))));
        assertDoesNotThrow(() -> validadorConsulta.validar(consulta(CenarioTeste.AGORA.plusDays(1))));
    }
}
