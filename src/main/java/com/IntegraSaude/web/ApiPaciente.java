package com.IntegraSaude.web;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Endereco;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.Servicos;
import com.IntegraSaude.web.VisaoLembretes.DadosPaciente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Rotas das abas do paciente: Agenda, Lembretes, Remédios, Consultas e Responsáveis. */
final class ApiPaciente {

    private static final String NOVO = "novo";

    private final Servicos servicos;
    private final VisaoLembretes visao;

    ApiPaciente(Servicos servicos, VisaoLembretes visao) {
        this.servicos = servicos;
        this.visao = visao;
    }

    void registrar(ServidorWeb servidor) {
        servidor.get("/api/paciente/agenda", this::agendaDoDia);
        servidor.get("/api/paciente/semana", this::agendaDaSemana);
        servidor.get("/api/paciente/lembretes", this::lembretesPendentes);
        servidor.post("/api/paciente/lembretes/confirmar", this::confirmar);
        servidor.get("/api/paciente/historico", this::historico);
        servidor.get("/api/paciente/medicamentos", this::medicamentos);
        servidor.post("/api/paciente/medicamentos", this::cadastrarMedicamento);
        servidor.post("/api/paciente/medicamentos/remover", this::removerMedicamento);
        servidor.get("/api/paciente/consultas", this::consultas);
        servidor.post("/api/paciente/consultas", this::cadastrarConsulta);
        servidor.post("/api/paciente/consultas/cancelar", this::cancelarConsulta);
        servidor.get("/api/paciente/responsaveis", this::responsaveis);
        servidor.post("/api/paciente/responsaveis", this::vincularResponsavel);
        servidor.post("/api/paciente/responsaveis/remover", this::desvincularResponsavel);
        servidor.get("/api/especialidades", this::especialidades);
        servidor.get("/api/cep", this::buscarCep);
    }

    // ---------------------------------------------------------------- Agenda

    private Object agendaDoDia(Requisicao requisicao) {
        DadosPaciente dados = visao.carregar(requisicao.exigirPaciente());
        LocalDate dia = requisicao.parametros().dataOpcional("dia").orElse(visao.hoje());
        return Json.objeto("hoje", visao.hoje(), "dia", dia,
                "agora", visao.devidoAgora(dados).orElse(null),
                "itens", visao.itensDoDia(dados, dia));
    }

    private Object agendaDaSemana(Requisicao requisicao) {
        DadosPaciente dados = visao.carregar(requisicao.exigirPaciente());
        LocalDate inicio = requisicao.parametros().dataOpcional("inicio").orElse(visao.hoje());
        return Json.objeto("hoje", visao.hoje(), "inicio", inicio, "dias", visao.semana(dados, inicio));
    }

    // ---------------------------------------------------------------- Lembretes

    private Object lembretesPendentes(Requisicao requisicao) {
        DadosPaciente dados = visao.carregar(requisicao.exigirPaciente());
        return Json.objeto("tempoLimite", visao.tempoLimiteMinutos(), "pendentes", visao.pendentes(dados));
    }

    // O LembreteService confere se o lembrete é do paciente e se o prazo não expirou (RN10)
    private Object confirmar(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        servicos.lembretes().confirmar(paciente.getId(), requisicao.parametros().id("id"));
        return null;
    }

    private Object historico(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        DadosPaciente dados = visao.carregar(paciente);
        List<String> responsaveis = servicos.vinculos().listarResponsaveisDoPaciente(paciente.getId()).stream()
                .map(u -> VisaoLembretes.primeiroNome(u.getNome()))
                .toList();
        return Json.objeto("hoje", visao.hoje(), "responsaveis", responsaveis, "itens", visao.historico(dados));
    }

    // ---------------------------------------------------------------- Medicamentos

    private Object medicamentos(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        LocalDate hoje = visao.hoje();
        return servicos.medicamentos().listarDoPaciente(paciente.getId()).stream()
                .map(m -> Json.objeto(
                        "id", m.getId(),
                        "nome", m.getNome(),
                        "dosagem", m.getDosagem(),
                        "frequencia", m.getFrequenciaHoras(),
                        "primeiroHorario", VisaoLembretes.hora(m.getDataInicio().atTime(m.getPrimeiroHorario())),
                        "inicio", m.getDataInicio(),
                        "fim", m.getDataFim(),
                        "encerrado", hoje.isAfter(m.getDataFim()),
                        "horarios", horariosDeReferencia(m, hoje)))
                .toList();
    }

    // Horários de um dia cheio do tratamento: hoje, ou o dia seguinte ao início (no primeiro dia a
    // sequência só começa no primeiro horário). Fora do período, o último dia dele.
    private List<String> horariosDeReferencia(Medicamento medicamento, LocalDate hoje) {
        LocalDate dia = hoje.isAfter(medicamento.getDataInicio()) ? hoje : medicamento.getDataInicio().plusDays(1);
        if (dia.isAfter(medicamento.getDataFim())) {
            dia = medicamento.getDataFim();
        }
        return servicos.calculadora().horariosDoDia(medicamento, dia).stream()
                .map(VisaoLembretes::hora)
                .toList();
    }

    private Object cadastrarMedicamento(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        Parametros p = requisicao.parametros();
        Medicamento medicamento = servicos.medicamentos().cadastrar(new Medicamento(paciente.getId(),
                p.texto("nome"), p.texto("dosagem"), p.inteiro("frequencia", "A cada (horas)"),
                p.hora("primeiroHorario", "Primeiro horario"), p.data("inicio", "Inicio"), p.data("fim", "Fim")));
        return Json.objeto("id", medicamento.getId());
    }

    private Object removerMedicamento(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        servicos.medicamentos().remover(paciente.getId(), requisicao.parametros().id("id"));
        return null;
    }

    // ---------------------------------------------------------------- Consultas

    // Próximas consultas agendadas primeiro; depois as passadas e canceladas, da mais recente
    private Object consultas(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        LocalDateTime agora = visao.agora();
        List<Consulta> todas = servicos.consultas().listarTodasDoPaciente(paciente.getId());
        Stream<Consulta> futuras = todas.stream().filter(c -> ehFutura(c, agora));
        Stream<Consulta> demais = todas.stream().filter(c -> !ehFutura(c, agora))
                .sorted(Comparator.comparing(Consulta::getDataHora).reversed());
        List<Map<String, Object>> lista = Stream.concat(futuras, demais)
                .map(c -> Json.objeto(
                        "id", c.getId(),
                        "especialidade", c.getMedico().getEspecialidade().getNome(),
                        "medico", c.getMedico().getNome(),
                        "data", c.getDataHora().toLocalDate(),
                        "hora", VisaoLembretes.hora(c.getDataHora()),
                        "local", c.getLocal(),
                        "status", c.getStatus(),
                        "podeCancelar", ehFutura(c, agora)))
                .toList();
        return Json.objeto("hoje", visao.hoje(), "consultas", lista);
    }

    private static boolean ehFutura(Consulta consulta, LocalDateTime agora) {
        return consulta.estaAgendada() && !consulta.getDataHora().isBefore(agora);
    }

    private Object cadastrarConsulta(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        Parametros p = requisicao.parametros();
        LocalDateTime dataHora = LocalDateTime.of(p.data("data", "Data"), p.hora("hora", "Horario"));
        Consulta consulta = servicos.consultas().cadastrar(
                new Consulta(paciente.getId(), escolherMedico(p), p.texto("local"), dataHora));
        return Json.objeto("id", consulta.getId());
    }

    // Usa a especialidade e o médico escolhidos ou cadastra novos (opção "novo" no formulário)
    private Medico escolherMedico(Parametros p) {
        String especialidadeEscolhida = p.texto("especialidade");
        if (especialidadeEscolhida.isEmpty()) {
            throw new DadoInvalidoException("Escolha a especialidade");
        }
        boolean especialidadeNova = NOVO.equals(especialidadeEscolhida);
        Especialidade especialidade = especialidadeNova
                ? servicos.especialidades().cadastrar(p.texto("novaEspecialidade"))
                : servicos.especialidades().buscarPorId(p.id("especialidade"));
        if (especialidadeNova || NOVO.equals(p.texto("medico"))) {
            return servicos.medicos().cadastrar(p.texto("novoMedico"), especialidade.getId());
        }
        if (p.texto("medico").isEmpty()) {
            throw new DadoInvalidoException("Escolha o medico");
        }
        Medico medico = servicos.medicos().buscarPorId(p.id("medico"));
        if (!medico.getEspecialidade().getId().equals(especialidade.getId())) {
            throw new DadoInvalidoException("O medico escolhido nao e desta especialidade");
        }
        return medico;
    }

    private Object cancelarConsulta(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        servicos.consultas().cancelar(paciente.getId(), requisicao.parametros().id("id"));
        return null;
    }

    private Object especialidades(Requisicao requisicao) {
        requisicao.exigirPaciente();
        return servicos.especialidades().listar().stream()
                .map(e -> Json.objeto("id", e.getId(), "nome", e.getNome(), "medicos",
                        servicos.medicos().listarPorEspecialidade(e.getId()).stream()
                                .map(m -> Json.objeto("id", m.getId(), "nome", m.getNome()))
                                .toList()))
                .toList();
    }

    // ViaCEP: falha de rede volta como erro 502 e a tela pede o endereço à mão
    private Object buscarCep(Requisicao requisicao) {
        requisicao.exigirPaciente();
        Endereco endereco = servicos.enderecos().buscarPorCep(requisicao.parametros().texto("cep"));
        return Json.objeto("cep", endereco.cep(), "logradouro", endereco.logradouro(), "bairro", endereco.bairro(),
                "cidade", endereco.cidade(), "uf", endereco.uf());
    }

    // ---------------------------------------------------------------- Responsáveis

    private Object responsaveis(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        return servicos.vinculos().listarResponsaveisDoPaciente(paciente.getId()).stream()
                .map(u -> Json.objeto("nome", u.getNome(), "email", u.getEmail()))
                .toList();
    }

    // O VinculoService garante que o e-mail é de um RESPONSAVEL e que o vínculo não se repete (RN03)
    private Object vincularResponsavel(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        Usuario responsavel = servicos.usuarios().buscarPorEmail(requisicao.parametros().texto("email"));
        servicos.vinculos().criar(paciente.getId(), responsavel.getId());
        return Json.objeto("nome", responsavel.getNome());
    }

    private Object desvincularResponsavel(Requisicao requisicao) {
        Usuario paciente = requisicao.exigirPaciente();
        Usuario responsavel = servicos.usuarios().buscarPorEmail(requisicao.parametros().texto("email"));
        servicos.vinculos().remover(paciente.getId(), responsavel.getId());
        return null;
    }
}
