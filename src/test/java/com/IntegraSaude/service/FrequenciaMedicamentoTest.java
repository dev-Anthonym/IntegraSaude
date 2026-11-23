package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.Usuario;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Frequência do medicamento (RN05/RN07): limites, horários gerados e manutenção do cadastro. */
class FrequenciaMedicamentoTest {

    private static final LocalDate HOJE = CenarioTeste.AGORA.toLocalDate();

    private CenarioTeste cenario;
    private Usuario paciente;
    private ValidadorMedicamento validador;

    @BeforeEach
    void preparar() {
        cenario = new CenarioTeste();
        paciente = cenario.criarPaciente("maria@teste.com");
        validador = new ValidadorMedicamento(cenario.relogio);
    }

    private Medicamento comFrequencia(int horas) {
        return cenario.medicamento(paciente.getId(), horas, LocalTime.of(8, 0));
    }

    @ParameterizedTest(name = "frequencia de {0}h e valida")
    @ValueSource(ints = {1, 2, 5, 7, 12, 23, 24})
    void deveAceitarFrequenciasDentroDoIntervalo(int horas) {
        assertDoesNotThrow(() -> validador.validar(comFrequencia(horas)));
    }

    @ParameterizedTest(name = "frequencia de {0}h e invalida")
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 25, 48, Integer.MAX_VALUE})
    void deveRejeitarFrequenciasForaDoIntervalo(int horas) {
        DadoInvalidoException erro = assertThrows(DadoInvalidoException.class, () -> validador.validar(comFrequencia(horas)));
        assertEquals("A frequencia deve estar entre 1 e 24 horas", erro.getMessage());
    }

    // Dia seguinte ao início: um dia cheio da sequência que começa às 08:00
    @ParameterizedTest(name = "a cada {0}h -> {1} doses no dia")
    @CsvSource({"1, 24", "2, 12", "3, 8", "4, 6", "6, 4", "8, 3", "12, 2", "24, 1"})
    void deveGerarQuantidadeDeDosesProporcionalAFrequencia(int horas, int doses) {
        List<LocalDateTime> horarios = cenario.calculadora.horariosDoDia(comFrequencia(horas), HOJE.plusDays(1));
        assertEquals(doses, horarios.size());
        for (int i = 1; i < horarios.size(); i++) {
            assertEquals(horas, java.time.Duration.between(horarios.get(i - 1), horarios.get(i)).toHours());
        }
    }

    @Test
    void frequenciaQueNaoDivide24DeveDeslocarOsHorariosEntreDias() {
        Medicamento aCada5 = comFrequencia(5);
        // Dia 1: 08, 13, 18, 23 | Dia 2: 04, 09, 14, 19 | Dia 3: 00, 05, ...
        assertEquals(List.of(8, 13, 18, 23), horas(cenario.calculadora.horariosDoDia(aCada5, HOJE)));
        assertEquals(List.of(4, 9, 14, 19), horas(cenario.calculadora.horariosDoDia(aCada5, HOJE.plusDays(1))));
        assertEquals(0, cenario.calculadora.horariosDoDia(aCada5, HOJE.plusDays(2)).getFirst().getHour());
    }

    @Test
    void tratamentoDeUmUnicoDiaDeveSerValido() {
        Medicamento umDia = new Medicamento(paciente.getId(), "Amoxicilina", "500 mg", 8, LocalTime.of(8, 0), HOJE, HOJE);
        assertDoesNotThrow(() -> cenario.medicamentos.cadastrar(umDia));
        // 08:00 e 16:00; a dose das 24:00 já seria o dia seguinte, fora do período
        assertEquals(List.of(8, 16), horas(cenario.calculadora.horariosDoDia(umDia, HOJE)));
        assertTrue(cenario.calculadora.horariosDoDia(umDia, HOJE.plusDays(1)).isEmpty());
        assertTrue(cenario.calculadora.proximoHorario(umDia, HOJE.atTime(23, 0)).isEmpty());
        assertEquals(HOJE.atTime(16, 0), cenario.calculadora.proximoHorario(umDia, HOJE.atTime(10, 0)).orElseThrow());
    }

    @Test
    void doseDas23hDeveFicarNoDiaCerto() {
        Medicamento noturno = cenario.medicamento(paciente.getId(), 24, LocalTime.of(23, 59));
        assertEquals(List.of(HOJE.atTime(23, 59)), cenario.calculadora.horariosDoDia(noturno, HOJE));
    }

    @Test
    void deveValidarCamposObrigatorios() {
        Long id = paciente.getId();
        LocalTime oito = LocalTime.of(8, 0);
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, null, "1 cp", 8, oito, HOJE, HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, " ", "1 cp", 8, oito, HOJE, HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, "Dipirona", "", 8, oito, HOJE, HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, "Dipirona", null, 8, oito, HOJE, HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, "Dipirona", "1 cp", 8, null, HOJE, HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, "Dipirona", "1 cp", 8, oito, null, HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, "Dipirona", "1 cp", 8, oito, HOJE, null)));
    }

    @Test
    void fimHojeEValidoEFimOntemNao() {
        Long id = paciente.getId();
        LocalTime oito = LocalTime.of(8, 0);
        assertDoesNotThrow(() -> validador.validar(new Medicamento(id, "D", "1", 8, oito, HOJE.minusDays(3), HOJE)));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar(new Medicamento(id, "D", "1", 8, oito, HOJE.minusDays(3), HOJE.minusDays(1))));
    }

    @Test
    void removerDeveApagarSoOsLembretesPendentes() {
        Medicamento remedio = cenario.medicamentos.cadastrar(
                cenario.medicamento(paciente.getId(), 12, LocalTime.of(10, 0)));
        List<Lembrete> gerados = cenario.gerador.gerarDoDia(paciente.getId(), HOJE);
        assertEquals(2, gerados.size());
        cenario.lembretes.confirmar(paciente.getId(), gerados.getFirst().getId());

        cenario.medicamentos.remover(paciente.getId(), remedio.getId());

        assertTrue(cenario.medicamentos.listarDoPaciente(paciente.getId()).isEmpty());
        List<Lembrete> restantes = cenario.lembretes.listarDoPaciente(paciente.getId());
        assertEquals(1, restantes.size());
        assertEquals(StatusLembrete.CONFIRMADO, restantes.getFirst().getStatus());
    }

    @Test
    void atualizarDeveTrocarAFrequenciaEDescartarPendentesAntigos() {
        Medicamento remedio = cenario.medicamentos.cadastrar(
                cenario.medicamento(paciente.getId(), 12, LocalTime.of(10, 0)));
        cenario.gerador.gerarDoDia(paciente.getId(), HOJE);

        Medicamento novo = cenario.medicamento(paciente.getId(), 6, LocalTime.of(10, 0));
        novo.setId(remedio.getId());
        cenario.medicamentos.atualizar(novo);

        assertTrue(cenario.lembretes.listarPendentes(paciente.getId()).isEmpty());
        assertEquals(3, cenario.gerador.gerarDoDia(paciente.getId(), HOJE).size(), "10h, 16h e 22h");
    }

    @Test
    void atualizacaoInvalidaNaoDeveAlterarNada() {
        Medicamento remedio = cenario.medicamentos.cadastrar(comFrequencia(12));
        Medicamento invalido = comFrequencia(0);
        invalido.setId(remedio.getId());
        assertThrows(DadoInvalidoException.class, () -> cenario.medicamentos.atualizar(invalido));
        assertEquals(12, cenario.medicamentos.listarDoPaciente(paciente.getId()).getFirst().getFrequenciaHoras());
    }

    @Test
    void naoDeveMexerEmMedicamentoDeOutroPaciente() {
        Medicamento remedio = cenario.medicamentos.cadastrar(comFrequencia(12));
        Usuario pedro = cenario.criarPaciente("pedro@teste.com");
        assertThrows(RegistroNaoEncontradoException.class,
                () -> cenario.medicamentos.remover(pedro.getId(), remedio.getId()));
        Medicamento invasor = cenario.medicamento(pedro.getId(), 6, LocalTime.of(8, 0));
        invasor.setId(remedio.getId());
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.medicamentos.atualizar(invasor));
        assertEquals(1, cenario.medicamentos.listarDoPaciente(paciente.getId()).size());
    }

    @Test
    void deveListarMedicamentosPorNomeSemDiferenciarMaiusculas() {
        Long id = paciente.getId();
        LocalTime oito = LocalTime.of(8, 0);
        cenario.medicamentos.cadastrar(new Medicamento(id, "losartana", "50 mg", 24, oito, HOJE, HOJE));
        cenario.medicamentos.cadastrar(new Medicamento(id, "Amoxicilina", "500 mg", 8, oito, HOJE, HOJE));
        cenario.medicamentos.cadastrar(new Medicamento(id, "Dipirona", "1 g", 6, oito, HOJE, HOJE));
        assertEquals(List.of("Amoxicilina", "Dipirona", "losartana"),
                cenario.medicamentos.listarDoPaciente(id).stream().map(Medicamento::getNome).toList());
    }

    private static List<Integer> horas(List<LocalDateTime> horarios) {
        return horarios.stream().map(LocalDateTime::getHour).toList();
    }
}
