package com.IntegraSaude.web;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.AgendaService;
import com.IntegraSaude.service.Servicos;
import com.IntegraSaude.web.VisaoLembretes.DadosPaciente;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rotas das abas do responsável: Pacientes, Lembretes e Agenda.
 * Tudo parte dos pacientes vinculados a ele, então nunca enxerga dados de outros pacientes.
 * O responsável só acompanha: quem confirma é o paciente.
 */
final class ApiResponsavel {

    // Ordem da aba "Em aberto": perdidos, depois pendentes, depois os que ainda vão acontecer
    private static final List<String> ORDEM_EM_ABERTO = List.of(
            StatusLembrete.PERDIDO.name(), StatusLembrete.PENDENTE.name(), VisaoLembretes.EM_BREVE);

    private final Servicos servicos;
    private final VisaoLembretes visao;

    ApiResponsavel(Servicos servicos, VisaoLembretes visao) {
        this.servicos = servicos;
        this.visao = visao;
    }

    void registrar(ServidorWeb servidor) {
        servidor.get("/api/responsavel/pacientes", this::pacientes);
        servidor.get("/api/responsavel/lembretes", this::lembretesEmAberto);
        servidor.get("/api/responsavel/historico", this::historico);
        servidor.get("/api/responsavel/agenda", this::agendaDoDia);
        servidor.get("/api/responsavel/semana", this::agendaDaSemana);
    }

    private List<DadosPaciente> pacientesDo(Requisicao requisicao) {
        Usuario responsavel = requisicao.exigirResponsavel();
        return servicos.vinculos().listarPacientesDoResponsavel(responsavel.getId()).stream()
                .map(visao::carregar)
                .toList();
    }

    // Cada paciente com a agenda de hoje e quantos lembretes perdeu; no topo, o alerta mais recente
    private Object pacientes(Requisicao requisicao) {
        LocalDate hoje = visao.hoje();
        List<Map<String, Object>> pacientes = new ArrayList<>();
        Map<String, Object> ultimoAlerta = null;
        for (DadosPaciente dados : pacientesDo(requisicao)) {
            List<Lembrete> perdidos = visao.perdidosDoDia(dados, hoje);
            pacientes.add(Json.objeto(
                    "nome", dados.paciente().getNome(),
                    "email", dados.paciente().getEmail(),
                    "perdidosHoje", perdidos.size(),
                    "itens", visao.itensDoDia(dados, hoje)));
            for (Lembrete perdido : perdidos) {
                Map<String, Object> detalhe = visao.detalhar(dados, perdido);
                if (detalhe.get("alertaAs") != null && (ultimoAlerta == null
                        || ((String) detalhe.get("alertaAs")).compareTo((String) ultimoAlerta.get("alertaAs")) >= 0)) {
                    ultimoAlerta = detalhe;
                }
            }
        }
        return Json.objeto("tempoLimite", visao.tempoLimiteMinutos(), "alerta", ultimoAlerta,
                "pacientes", pacientes);
    }

    // Perdidos de hoje, pendentes e as próximas doses de hoje de todos os pacientes
    private Object lembretesEmAberto(Requisicao requisicao) {
        LocalDate hoje = visao.hoje();
        List<Map<String, Object>> itens = new ArrayList<>();
        for (DadosPaciente dados : pacientesDo(requisicao)) {
            for (Lembrete perdido : visao.perdidosDoDia(dados, hoje)) {
                itens.add(visao.detalhar(dados, perdido));
            }
            itens.addAll(visao.pendentes(dados));
            for (Map<String, Object> item : visao.itensDoDia(dados, hoje)) {
                if (VisaoLembretes.EM_BREVE.equals(item.get("status"))) {
                    itens.add(Json.objeto(
                            "paciente", dados.paciente().getNome(),
                            "primeiroNome", VisaoLembretes.primeiroNome(dados.paciente().getNome()),
                            "tipo", item.get("tipo"),
                            "status", VisaoLembretes.EM_BREVE,
                            "titulo", item.get("titulo"),
                            "subtitulo", item.get("subtitulo"),
                            "previsto", item.get("hora")));
                }
            }
        }
        itens.sort(Comparator
                .comparing((Map<String, Object> i) -> ORDEM_EM_ABERTO.indexOf(String.valueOf(i.get("status"))))
                .thenComparing(i -> String.valueOf(i.get("previsto"))));
        return Json.objeto("itens", itens);
    }

    // Confirmados e perdidos de todos os pacientes, do mais recente para o mais antigo
    private Object historico(Requisicao requisicao) {
        List<Map<String, Object>> itens = new ArrayList<>();
        for (DadosPaciente dados : pacientesDo(requisicao)) {
            itens.addAll(visao.historico(dados));
        }
        itens.sort(Comparator.comparing((Map<String, Object> i) -> i.get("data") + " " + i.get("previsto"))
                .reversed());
        return Json.objeto("hoje", visao.hoje(), "itens", itens);
    }

    private Object agendaDoDia(Requisicao requisicao) {
        LocalDate dia = requisicao.parametros().dataOpcional("dia").orElse(visao.hoje());
        List<Map<String, Object>> pacientes = pacientesDo(requisicao).stream()
                .map(dados -> Json.objeto("nome", dados.paciente().getNome(),
                        "itens", visao.itensDoDia(dados, dia)))
                .toList();
        return Json.objeto("hoje", visao.hoje(), "dia", dia, "pacientes", pacientes);
    }

    // Resumo da semana: quantos remédios e consultas cada paciente tem em cada dia
    private Object agendaDaSemana(Requisicao requisicao) {
        LocalDate inicio = requisicao.parametros().dataOpcional("inicio").orElse(visao.hoje());
        List<DadosPaciente> pacientes = pacientesDo(requisicao);
        List<List<Map<String, Object>>> semanas = pacientes.stream()
                .map(dados -> visao.semana(dados, inicio))
                .toList();
        List<Map<String, Object>> dias = new ArrayList<>();
        for (int i = 0; i < AgendaService.DIAS_DA_SEMANA; i++) {
            List<Map<String, Object>> linhas = new ArrayList<>();
            for (int p = 0; p < pacientes.size(); p++) {
                Map<String, Object> dia = semanas.get(p).get(i);
                linhas.add(Json.objeto(
                        "paciente", VisaoLembretes.primeiroNome(pacientes.get(p).paciente().getNome()),
                        "doses", dia.get("doses"),
                        "consultas", ((List<?>) dia.get("consultas")).size()));
            }
            dias.add(Json.objeto("data", inicio.plusDays(i), "linhas", linhas));
        }
        return Json.objeto("hoje", visao.hoje(), "inicio", inicio, "dias", dias);
    }
}
