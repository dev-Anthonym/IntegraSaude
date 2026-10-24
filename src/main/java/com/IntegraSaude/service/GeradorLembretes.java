package com.IntegraSaude.service;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.MedicamentoAgendado;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.LembreteRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

// Gera os lembretes do dia a partir dos medicamentos e consultas do paciente
public class GeradorLembretes {

    private final FonteMedicamentos fonteMedicamentos;
    private final HorariosMedicamento horariosMedicamento;
    private final FonteConsultas fonteConsultas;
    private final LembreteRepository lembreteRepository;
    private final Duration antecedenciaConsulta;

    public GeradorLembretes(FonteMedicamentos fonteMedicamentos, HorariosMedicamento horariosMedicamento,
                            FonteConsultas fonteConsultas, LembreteRepository lembreteRepository,
                            Duration antecedenciaConsulta) {
        this.fonteMedicamentos = fonteMedicamentos;
        this.horariosMedicamento = horariosMedicamento;
        this.fonteConsultas = fonteConsultas;
        this.lembreteRepository = lembreteRepository;
        this.antecedenciaConsulta = antecedenciaConsulta;
    }

    public List<Lembrete> gerarDoDia(Long pacienteId, LocalDate dia) {
        List<Lembrete> gerados = new ArrayList<>();
        for (MedicamentoAgendado medicamento : fonteMedicamentos.listarDoPaciente(pacienteId)) {
            if (medicamento.estaAtivoEm(dia)) {
                gerados.addAll(gerarDoMedicamento(pacienteId, medicamento, dia));
            }
        }
        return gerados;
    }

    private List<Lembrete> gerarDoMedicamento(Long pacienteId, MedicamentoAgendado medicamento, LocalDate dia) {
        List<Lembrete> gerados = new ArrayList<>();
        for (LocalTime horario : horariosMedicamento.horariosDoDia(medicamento.id(), dia)) {
            gerados.add(criar(pacienteId, TipoLembrete.MEDICAMENTO, medicamento.id(),
                    medicamento.nome(), dia.atTime(horario)));
        }
        return gerados;
    }

    private Lembrete criar(Long pacienteId, TipoLembrete tipo, Long itemId,
                           String descricao, LocalDateTime horario) {
        Lembrete lembrete = new Lembrete(null, pacienteId, tipo, itemId, descricao, horario);
        return lembreteRepository.salvar(lembrete);
    }
}
