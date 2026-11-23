package com.IntegraSaude.web;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.ItemAgenda;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.AgendaService;
import com.IntegraSaude.service.Servicos;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transforma os dados dos serviços no formato que as telas usam (títulos, horários e status).
 * É compartilhada pelas telas do paciente e do responsável. Não altera nada: só lê.
 */
final class VisaoLembretes {

    // Status extras, que não existem no domínio, usados só para exibir a agenda
    static final String EM_BREVE = "EM_BREVE";
    static final String SEM_REGISTRO = "SEM_REGISTRO";
    static final String AGENDADA = "AGENDADA";

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    // Tudo o que as telas precisam de um paciente, lido uma vez por requisição
    record DadosPaciente(Usuario paciente, Map<Long, Medicamento> medicamentos, Map<Long, Consulta> consultas,
                         List<Lembrete> lembretes, Map<Long, List<EventoHistorico>> eventosPorLembrete) {
    }

    private final Servicos servicos;

    VisaoLembretes(Servicos servicos) {
        this.servicos = servicos;
    }

    DadosPaciente carregar(Usuario paciente) {
        Long id = paciente.getId();
        Map<Long, Medicamento> medicamentos = servicos.medicamentos().listarDoPaciente(id).stream()
                .collect(Collectors.toMap(Medicamento::getId, Function.identity()));
        Map<Long, Consulta> consultas = servicos.consultas().listarTodasDoPaciente(id).stream()
                .collect(Collectors.toMap(Consulta::getId, Function.identity()));
        Map<Long, List<EventoHistorico>> eventos = servicos.historico().listarDoPaciente(id).stream()
                .filter(e -> e.getLembreteId() != null)
                .collect(Collectors.groupingBy(EventoHistorico::getLembreteId));
        return new DadosPaciente(paciente, medicamentos, consultas, servicos.lembretes().listarDoPaciente(id),
                eventos);
    }

    LocalDateTime agora() {
        return LocalDateTime.now(servicos.relogio());
    }

    LocalDate hoje() {
        return agora().toLocalDate();
    }

    int tempoLimiteMinutos() {
        return servicos.configuracao().tempoLimiteMinutos();
    }

    static String hora(LocalDateTime dataHora) {
        return dataHora.format(HORA);
    }

    static String primeiroNome(String nome) {
        String limpo = nome.trim();
        int espaco = limpo.indexOf(' ');
        return espaco < 0 ? limpo : limpo.substring(0, espaco);
    }

    // ---------------------------------------------------------------- Agenda

    // Linhas da agenda de um dia, cada uma com o status do lembrete correspondente
    List<Map<String, Object>> itensDoDia(DadosPaciente dados, LocalDate dia) {
        LocalDateTime agora = agora();
        List<Map<String, Object>> itens = new ArrayList<>();
        for (ItemAgenda item : servicos.agenda().agendaDoDia(dados.paciente().getId(), dia)) {
            if (item.tipo() == TipoLembrete.MEDICAMENTO) {
                Medicamento medicamento = dados.medicamentos().get(item.itemId());
                Optional<Lembrete> lembrete = lembreteDo(dados, item);
                // Antes do horário a dose aparece "Em breve", mesmo que o lembrete já tenha sido gerado
                boolean futuro = item.dataHora().isAfter(agora);
                String status = lembrete.filter(l -> !(futuro && l.estaPendente()))
                        .map(l -> l.getStatus().name())
                        .orElse(futuro ? EM_BREVE : SEM_REGISTRO);
                String nome = medicamento == null ? item.descricao() : medicamento.getNome();
                itens.add(Json.objeto(
                        "hora", hora(item.dataHora()),
                        "tipo", item.tipo(),
                        "titulo", medicamento == null ? nome : nome + " " + medicamento.getDosagem(),
                        "nome", nome,
                        "subtitulo", "Medicamento",
                        "status", status,
                        "lembreteId", lembrete.map(Lembrete::getId).orElse(null)));
            } else {
                Consulta consulta = dados.consultas().get(item.itemId());
                itens.add(Json.objeto(
                        "hora", hora(item.dataHora()),
                        "tipo", item.tipo(),
                        "titulo", consulta == null ? item.descricao()
                                : consulta.getMedico().getEspecialidade().getNome(),
                        "nome", "Consulta",
                        "subtitulo", consulta == null ? "Consulta" : "Consulta · " + consulta.getMedico().getNome(),
                        "status", AGENDADA,
                        "lembreteId", null));
            }
        }
        return itens;
    }

    // Os 7 dias a partir de "inicio", com os horários de remédio e as consultas de cada dia
    List<Map<String, Object>> semana(DadosPaciente dados, LocalDate inicio) {
        Map<LocalDate, List<ItemAgenda>> porDia = servicos.agenda()
                .agendaDaSemana(dados.paciente().getId(), inicio).stream()
                .collect(Collectors.groupingBy(i -> i.dataHora().toLocalDate()));
        List<Map<String, Object>> dias = new ArrayList<>();
        for (int i = 0; i < AgendaService.DIAS_DA_SEMANA; i++) {
            LocalDate dia = inicio.plusDays(i);
            List<ItemAgenda> itens = porDia.getOrDefault(dia, List.of());
            TreeSet<String> horariosRemedios = new TreeSet<>();
            List<Map<String, Object>> consultas = new ArrayList<>();
            int doses = 0;
            for (ItemAgenda item : itens) {
                if (item.tipo() == TipoLembrete.MEDICAMENTO) {
                    horariosRemedios.add(hora(item.dataHora()));
                    doses++;
                } else {
                    Consulta consulta = dados.consultas().get(item.itemId());
                    consultas.add(Json.objeto("hora", hora(item.dataHora()), "titulo",
                            consulta == null ? item.descricao() : consulta.getMedico().getEspecialidade().getNome()));
                }
            }
            dias.add(Json.objeto("data", dia, "horariosRemedios", List.copyOf(horariosRemedios),
                    "doses", doses, "consultas", consultas));
        }
        return dias;
    }

    // O lembrete de medicamento gerado para aquela linha da agenda (mesmo item e horário)
    private Optional<Lembrete> lembreteDo(DadosPaciente dados, ItemAgenda item) {
        return dados.lembretes().stream()
                .filter(l -> l.getTipo() == item.tipo() && l.getItemId().equals(item.itemId())
                        && l.getHorarioPrevisto().equals(item.dataHora()))
                .findFirst();
    }

    // ---------------------------------------------------------------- Lembretes

    // Lembrete com título, horários, prazo e os momentos de confirmação e de alerta
    Map<String, Object> detalhar(DadosPaciente dados, Lembrete lembrete) {
        String titulo = lembrete.getDescricao();
        String subtitulo = "";
        String consultaAs = null;
        if (lembrete.getTipo() == TipoLembrete.MEDICAMENTO) {
            Medicamento medicamento = dados.medicamentos().get(lembrete.getItemId());
            if (medicamento != null) {
                titulo = medicamento.getNome() + " " + medicamento.getDosagem();
                subtitulo = "Medicamento";
            }
        } else {
            Consulta consulta = dados.consultas().get(lembrete.getItemId());
            if (consulta != null) {
                titulo = consulta.getMedico().getEspecialidade().getNome();
                subtitulo = consulta.getMedico().getNome();
                consultaAs = hora(consulta.getDataHora());
            }
        }
        LocalDateTime previsto = lembrete.getHorarioPrevisto();
        return Json.objeto(
                "id", lembrete.getId(),
                "paciente", dados.paciente().getNome(),
                "primeiroNome", primeiroNome(dados.paciente().getNome()),
                "tipo", lembrete.getTipo(),
                "status", lembrete.getStatus(),
                "titulo", titulo,
                "subtitulo", subtitulo,
                "consultaAs", consultaAs,
                "data", previsto.toLocalDate(),
                "previsto", hora(previsto),
                "prazo", hora(previsto.plusMinutes(tempoLimiteMinutos())),
                "devido", !previsto.isAfter(agora()),
                "confirmadoAs", momentoDoEvento(dados, lembrete, TipoEvento.CONFIRMADO),
                "alertaAs", momentoDoEvento(dados, lembrete, TipoEvento.ALERTA_ENVIADO));
    }

    private String momentoDoEvento(DadosPaciente dados, Lembrete lembrete, TipoEvento tipo) {
        return dados.eventosPorLembrete().getOrDefault(lembrete.getId(), List.of()).stream()
                .filter(e -> e.getTipo() == tipo)
                .map(e -> hora(e.getDataHora()))
                .findFirst().orElse(null);
    }

    // Pendentes, do mais antigo para o mais novo
    List<Map<String, Object>> pendentes(DadosPaciente dados) {
        return dados.lembretes().stream()
                .filter(Lembrete::estaPendente)
                .map(l -> detalhar(dados, l))
                .toList();
    }

    // Primeiro pendente cujo horário já chegou (o cartão "Agora" da agenda)
    Optional<Map<String, Object>> devidoAgora(DadosPaciente dados) {
        LocalDateTime agora = agora();
        return dados.lembretes().stream()
                .filter(l -> l.estaPendente() && !l.getHorarioPrevisto().isAfter(agora))
                .findFirst()
                .map(l -> detalhar(dados, l));
    }

    // Confirmados e perdidos, do mais recente para o mais antigo
    List<Map<String, Object>> historico(DadosPaciente dados) {
        return dados.lembretes().stream()
                .filter(l -> !l.estaPendente())
                .sorted(Comparator.comparing(Lembrete::getHorarioPrevisto).reversed())
                .map(l -> detalhar(dados, l))
                .toList();
    }

    // Perdidos do dia informado
    List<Lembrete> perdidosDoDia(DadosPaciente dados, LocalDate dia) {
        return dados.lembretes().stream()
                .filter(l -> l.getStatus() == StatusLembrete.PERDIDO
                        && l.getHorarioPrevisto().toLocalDate().equals(dia))
                .toList();
    }
}
