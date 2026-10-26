package com.IntegraSaude.service;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.DescricaoLembrete;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.ConsultaRepository;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.MedicamentoRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Gera os lembretes do dia: um por horário de medicamento e um por consulta (RN12 evita duplicados). */
public class GeradorLembretes {

    private final MedicamentoRepository medicamentos;
    private final ConsultaRepository consultas;
    private final LembreteRepository lembretes;
    private final CalculadoraHorarios calculadora;
    private final HistoricoService historico;
    private final Clock relogio;
    private final ConfiguracaoLembretes configuracao;

    public GeradorLembretes(MedicamentoRepository medicamentos, ConsultaRepository consultas,
                            LembreteRepository lembretes, CalculadoraHorarios calculadora,
                            HistoricoService historico, Clock relogio, ConfiguracaoLembretes configuracao) {
        this.medicamentos = medicamentos;
        this.consultas = consultas;
        this.lembretes = lembretes;
        this.calculadora = calculadora;
        this.historico = historico;
        this.relogio = relogio;
        this.configuracao = configuracao;
    }

    // Gera usando o relógio injetado como "agora"
    public List<Lembrete> gerarDoDia(Long pacienteId, LocalDate dia) {
        return gerarDoDia(pacienteId, dia, LocalDateTime.now(relogio));
    }

    // Gera os lembretes do dia e devolve só os que foram criados agora
    public List<Lembrete> gerarDoDia(Long pacienteId, LocalDate dia, LocalDateTime agora) {
        List<Lembrete> criados = new ArrayList<>();
        // Um lembrete por horário de cada medicamento do paciente
        for (Medicamento medicamento : medicamentos.listarPorPaciente(pacienteId)) {
            for (LocalDateTime horario : calculadora.horariosDoDia(medicamento, dia)) {
                criarSeNovo(pacienteId, TipoLembrete.MEDICAMENTO, medicamento.getId(), horario,
                        descreverMedicamento(medicamento), agora).ifPresent(criados::add);
            }
        }
        // Um lembrete por consulta, com antecedência configurável
        for (Consulta consulta : consultas.listarPorPaciente(pacienteId)) {
            if (!consulta.estaAgendada()) {
                continue;
            }
            LocalDateTime horario = consulta.getDataHora().minusMinutes(configuracao.antecedenciaConsultaMinutos());
            if (horario.toLocalDate().equals(dia)) {
                criarSeNovo(pacienteId, TipoLembrete.CONSULTA, consulta.getId(), horario,
                        descreverConsulta(consulta), agora).ifPresent(criados::add);
            }
        }
        return criados;
    }

    // Cria o lembrete se ele ainda não existe (RN12) e não está vencido; registra no histórico (RN13)
    private Optional<Lembrete> criarSeNovo(Long pacienteId, TipoLembrete tipo, Long itemId,
                                                     LocalDateTime horario, String descricao, LocalDateTime agora) {
        boolean jaVenceu = agora.isAfter(horario.plusMinutes(configuracao.tempoLimiteMinutos()));
        if (jaVenceu || lembretes.existe(pacienteId, tipo, itemId, horario)) {
            return Optional.empty();
        }
        Lembrete lembrete = lembretes.salvar(new Lembrete(pacienteId, tipo, itemId, horario, descricao));
        historico.registrar(pacienteId, lembrete.getId(), TipoEvento.GERADO,
                DescricaoLembrete.evento(TipoEvento.GERADO, descricao));
        return Optional.of(lembrete);
    }

    private String descreverMedicamento(Medicamento medicamento) {
        return DescricaoLembrete.medicamento(medicamento.getNome(), medicamento.getDosagem());
    }

    private String descreverConsulta(Consulta consulta) {
        return DescricaoLembrete.consulta(consulta.getMedico().getEspecialidade().getNome(),
                consulta.getMedico().getNome(), consulta.getLocal(), consulta.getDataHora());
    }
}
