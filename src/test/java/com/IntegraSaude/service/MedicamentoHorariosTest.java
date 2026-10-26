package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/** RN05 a RN07: serviço de medicamentos e cálculo de horários. */
class MedicamentoHorariosTest {

    private static final LocalDate HOJE = CenarioTeste.AGORA.toLocalDate();

    private final CenarioTeste c = new CenarioTeste();
    private final CalculadoraHorarios calculadora = new CalculadoraHorarios();

    private Medicamento medicamento(int frequencia, LocalTime primeiro, LocalDate inicio, LocalDate fim) {
        return new Medicamento(1L, "Remedio", "10 mg", frequencia, primeiro, inicio, fim);
    }

    @Test
    void deveCalcularHorariosACadaOitoHoras() {
        Medicamento m = medicamento(8, LocalTime.of(6, 0), HOJE, HOJE.plusDays(3));
        assertEquals(List.of(HOJE.atTime(6, 0), HOJE.atTime(14, 0), HOJE.atTime(22, 0)),
                calculadora.horariosDoDia(m, HOJE));
    }

    @Test
    void deveGerarUmUnicoHorarioParaFrequenciaDe24Horas() {
        Medicamento m = medicamento(24, LocalTime.of(8, 0), HOJE, HOJE.plusDays(3));
        assertEquals(List.of(HOJE.atTime(8, 0)), calculadora.horariosDoDia(m, HOJE));
        assertEquals(List.of(HOJE.plusDays(1).atTime(8, 0)), calculadora.horariosDoDia(m, HOJE.plusDays(1)));
    }

    @Test
    void naoDeveGerarHorariosAntesDoInicio() {
        Medicamento m = medicamento(8, LocalTime.of(6, 0), HOJE, HOJE.plusDays(3));
        assertTrue(calculadora.horariosDoDia(m, HOJE.minusDays(1)).isEmpty());
    }

    @Test
    void naoDeveGerarHorariosDepoisDoFim() {
        Medicamento m = medicamento(8, LocalTime.of(22, 0), HOJE, HOJE.plusDays(1));
        assertTrue(calculadora.horariosDoDia(m, HOJE.plusDays(2)).isEmpty());
        // No último dia o horário das 22:00 ainda vale
        assertEquals(List.of(HOJE.plusDays(1).atTime(6, 0), HOJE.plusDays(1).atTime(14, 0),
                HOJE.plusDays(1).atTime(22, 0)), calculadora.horariosDoDia(m, HOJE.plusDays(1)));
    }

    @Test
    void deveTratarAViradaDeDia() {
        // 20:00 a cada 6h: 20:00 (dia 1) e 02:00, 08:00, 14:00, 20:00 (dia 2)
        Medicamento m = medicamento(6, LocalTime.of(20, 0), HOJE, HOJE.plusDays(1));
        assertEquals(List.of(HOJE.atTime(20, 0)), calculadora.horariosDoDia(m, HOJE));
        assertEquals(List.of(HOJE.plusDays(1).atTime(2, 0), HOJE.plusDays(1).atTime(8, 0),
                HOJE.plusDays(1).atTime(14, 0), HOJE.plusDays(1).atTime(20, 0)),
                calculadora.horariosDoDia(m, HOJE.plusDays(1)));
    }

    @Test
    void deveRetornarHorariosOrdenados() {
        Medicamento m = medicamento(5, LocalTime.of(7, 30), HOJE, HOJE.plusDays(5));
        List<LocalDateTime> horarios = calculadora.horariosDoDia(m, HOJE.plusDays(2));
        List<LocalDateTime> ordenados = new ArrayList<>(horarios);
        Collections.sort(ordenados);
        assertEquals(ordenados, horarios);
    }

    @Test
    void deveCalcularProximoHorario() {
        Medicamento m = medicamento(8, LocalTime.of(6, 0), HOJE, HOJE.plusDays(3));
        assertEquals(HOJE.atTime(14, 0), calculadora.proximoHorario(m, HOJE.atTime(10, 0)).orElseThrow());
        assertTrue(calculadora.proximoHorario(m, HOJE.plusDays(10).atTime(0, 0)).isEmpty());
    }

    @Test
    void deveCadastrarMedicamentoParaPaciente() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Medicamento salvo = c.medicamentos.cadastrar(c.medicamento(paciente.getId(), 8, LocalTime.of(8, 0)));
        assertEquals(1, c.medicamentos.listarDoPaciente(paciente.getId()).size());
        assertTrue(salvo.getId() != null);
    }

    @Test
    void deveRejeitarMedicamentoParaUsuarioQueNaoEPaciente() {
        Usuario responsavel = c.criarResponsavel("r@email.com");
        assertThrows(DadoInvalidoException.class,
                () -> c.medicamentos.cadastrar(c.medicamento(responsavel.getId(), 8, LocalTime.of(8, 0))));
    }
}
