package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.ConsultaAgendada;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.MedicamentoAgendado;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeradorLembretesTest {

    private static final Long PACIENTE_ID = 10L;
    private static final LocalDate DIA = LocalDate.of(2026, 10, 24);
    private static final Duration ANTECEDENCIA = Duration.ofMinutes(60);

    private final Map<Long, List<MedicamentoAgendado>> medicamentos = new HashMap<>();
    private final Map<Long, List<LocalTime>> horarios = new HashMap<>();
    private final Map<Long, List<ConsultaAgendada>> consultas = new HashMap<>();
    private LembreteRepository repository;
    private GeradorLembretes gerador;

    @BeforeEach
    void setUp() {
        repository = new LembreteRepositoryMemoria();
        gerador = new GeradorLembretes(
                pacienteId -> medicamentos.getOrDefault(pacienteId, List.of()),
                (medicamentoId, dia) -> horarios.getOrDefault(medicamentoId, List.of()),
                pacienteId -> consultas.getOrDefault(pacienteId, List.of()),
                repository,
                new HistoricoService(new HistoricoRepositoryMemoria(), repository, Clock.systemDefaultZone()),
                ANTECEDENCIA);
    }

    @Test
    void deveGerarUmLembretePorHorarioDeMedicamento() {
        cadastrarMedicamento(new MedicamentoAgendado(5L, "Losartana 50mg", DIA, null),
                LocalTime.of(8, 0), LocalTime.of(20, 0));

        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertEquals(2, gerados.size());
        assertEquals(DIA.atTime(8, 0), gerados.get(0).getHorarioPrevisto());
        assertEquals(DIA.atTime(20, 0), gerados.get(1).getHorarioPrevisto());
        assertTrue(gerados.stream().allMatch(l -> l.getTipo() == TipoLembrete.MEDICAMENTO
                && l.getItemId().equals(5L)
                && l.getDescricao().equals("Losartana 50mg")
                && l.getStatus() == StatusLembrete.PENDENTE));
        assertEquals(2, repository.listarPorPaciente(PACIENTE_ID).size());
    }

    @Test
    void naoDeveGerarLembretesParaMedicamentoForaDoPeriodo() {
        cadastrarMedicamento(new MedicamentoAgendado(5L, "Amoxicilina", DIA.minusDays(10), DIA.minusDays(1)),
                LocalTime.of(8, 0));
        cadastrarMedicamento(new MedicamentoAgendado(6L, "Dipirona", DIA.plusDays(1), null),
                LocalTime.of(9, 0));

        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertTrue(gerados.isEmpty());
    }

    @Test
    void deveGerarNadaQuandoNaoHaItens() {
        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertTrue(gerados.isEmpty());
        assertTrue(repository.listarPorPaciente(PACIENTE_ID).isEmpty());
    }

    @Test
    void deveGerarLembreteDeConsultaComAntecedencia() {
        cadastrarConsulta(new ConsultaAgendada(7L, "Consulta com cardiologista", DIA.atTime(14, 0)));

        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertEquals(1, gerados.size());
        Lembrete lembrete = gerados.get(0);
        assertEquals(TipoLembrete.CONSULTA, lembrete.getTipo());
        assertEquals(7L, lembrete.getItemId());
        assertEquals("Consulta com cardiologista", lembrete.getDescricao());
        assertEquals(DIA.atTime(13, 0), lembrete.getHorarioPrevisto());
    }

    @Test
    void naoDeveGerarLembreteDeConsultaDeOutroDia() {
        cadastrarConsulta(new ConsultaAgendada(7L, "Retorno", DIA.plusDays(3).atTime(14, 0)));

        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertTrue(gerados.isEmpty());
    }

    @Test
    void deveGerarNoDiaAnteriorLembreteDeConsultaLogoAposMeiaNoite() {
        cadastrarConsulta(new ConsultaAgendada(7L, "Exame em jejum", DIA.plusDays(1).atTime(0, 30)));

        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertEquals(1, gerados.size());
        assertEquals(DIA.atTime(23, 30), gerados.get(0).getHorarioPrevisto());
    }

    @Test
    void naoDeveGerarLembreteDuplicado() {
        cadastrarMedicamento(new MedicamentoAgendado(5L, "Losartana 50mg", DIA, null), LocalTime.of(8, 0));
        cadastrarConsulta(new ConsultaAgendada(7L, "Consulta com cardiologista", DIA.atTime(14, 0)));
        gerador.gerarDoDia(PACIENTE_ID, DIA);

        List<Lembrete> geradosNaSegundaVez = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertTrue(geradosNaSegundaVez.isEmpty());
        assertEquals(2, repository.listarPorPaciente(PACIENTE_ID).size());
    }

    @Test
    void naoDeveGerarLembreteDuplicadoQuandoHorarioSeRepete() {
        cadastrarMedicamento(new MedicamentoAgendado(5L, "Losartana 50mg", DIA, null),
                LocalTime.of(8, 0), LocalTime.of(8, 0));

        List<Lembrete> gerados = gerador.gerarDoDia(PACIENTE_ID, DIA);

        assertEquals(1, gerados.size());
    }

    private void cadastrarConsulta(ConsultaAgendada consulta) {
        consultas.computeIfAbsent(PACIENTE_ID, id -> new ArrayList<>()).add(consulta);
    }

    private void cadastrarMedicamento(MedicamentoAgendado medicamento, LocalTime... horariosDoDia) {
        medicamentos.computeIfAbsent(PACIENTE_ID, id -> new ArrayList<>()).add(medicamento);
        horarios.put(medicamento.id(), List.of(horariosDoDia));
    }
}
