package com.IntegraSaude.service;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.ItemAgenda;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.ConsultaRepository;
import com.IntegraSaude.repository.MedicamentoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Monta a agenda misturando medicamentos e consultas, ordenada por data e hora (RN14). */
public class AgendaService {

    // A agenda semanal cobre 7 dias a partir da data informada
    public static final int DIAS_DA_SEMANA = 7;

    private final MedicamentoRepository medicamentos;
    private final ConsultaRepository consultas;
    private final CalculadoraHorarios calculadora;

    public AgendaService(MedicamentoRepository medicamentos, ConsultaRepository consultas,
                         CalculadoraHorarios calculadora) {
        this.medicamentos = medicamentos;
        this.consultas = consultas;
        this.calculadora = calculadora;
    }

    // Agenda de um único dia
    public List<ItemAgenda> agendaDoDia(Long pacienteId, LocalDate dia) {
        List<ItemAgenda> itens = new ArrayList<>();
        // Doses dos medicamentos naquele dia
        for (Medicamento medicamento : medicamentos.listarPorPaciente(pacienteId)) {
            for (LocalDateTime horario : calculadora.horariosDoDia(medicamento, dia)) {
                itens.add(new ItemAgenda(horario, TipoLembrete.MEDICAMENTO,
                        "Tomar " + medicamento.getNome() + " (" + medicamento.getDosagem() + ")"));
            }
        }
        // Consultas naquele dia
        for (Consulta consulta : consultas.listarPorPaciente(pacienteId)) {
            if (consulta.estaAgendada() && consulta.getDataHora().toLocalDate().equals(dia)) {
                itens.add(new ItemAgenda(consulta.getDataHora(), TipoLembrete.CONSULTA,
                        "Consulta de " + consulta.getMedico().getEspecialidade().getNome()
                                + " com " + consulta.getMedico().getNome() + " em " + consulta.getLocal()));
            }
        }
        return ordenar(itens);
    }

    // Agenda de 7 dias a partir de "inicio"
    public List<ItemAgenda> agendaDaSemana(Long pacienteId, LocalDate inicio) {
        List<ItemAgenda> itens = new ArrayList<>();
        for (int deslocamento = 0; deslocamento < DIAS_DA_SEMANA; deslocamento++) {
            itens.addAll(agendaDoDia(pacienteId, inicio.plusDays(deslocamento)));
        }
        return ordenar(itens);
    }

    private List<ItemAgenda> ordenar(List<ItemAgenda> itens) {
        return itens.stream()
                .sorted(Comparator.comparing(ItemAgenda::dataHora).thenComparing(ItemAgenda::tipo))
                .toList();
    }
}
