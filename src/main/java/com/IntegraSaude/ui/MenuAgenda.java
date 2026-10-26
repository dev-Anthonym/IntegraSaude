package com.IntegraSaude.ui;

import com.IntegraSaude.model.ItemAgenda;
import com.IntegraSaude.service.AgendaService;
import java.time.LocalDate;
import java.util.List;

/** Agenda do dia e da semana (medicamentos e consultas juntos). */
public class MenuAgenda extends MenuBase {

    private final AgendaService agenda;
    private final SelecaoPaciente selecao;

    public MenuAgenda(LeitorConsole leitor, SaidaConsole saida, AgendaService agenda, SelecaoPaciente selecao) {
        super(leitor, saida);
        this.agenda = agenda;
        this.selecao = selecao;
    }

    public void executar() {
        executarMenu(() -> "Agenda", "Voltar", () -> List.of(
                new Opcao("Agenda do dia", this::agendaDoDia),
                new Opcao("Agenda da semana (7 dias)", this::agendaDaSemana)));
    }

    private void agendaDoDia() {
        selecao.escolher().ifPresent(paciente -> {
            LocalDate dia = leitor.lerDataOpcional("Dia").orElse(LocalDate.now());
            exibir(agenda.agendaDoDia(paciente.getId(), dia));
        });
    }

    private void agendaDaSemana() {
        selecao.escolher().ifPresent(paciente -> {
            LocalDate inicio = leitor.lerDataOpcional("Primeiro dia").orElse(LocalDate.now());
            exibir(agenda.agendaDaSemana(paciente.getId(), inicio));
        });
    }

    private void exibir(List<ItemAgenda> itens) {
        if (itens.isEmpty()) {
            saida.exibir("Agenda vazia.");
        }
        itens.forEach(i -> saida.exibir(i.dataHora().format(Formatos.DATA_HORA)
                + " | " + i.tipo() + " | " + i.descricao()));
    }
}
