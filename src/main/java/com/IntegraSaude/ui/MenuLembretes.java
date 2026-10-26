package com.IntegraSaude.ui;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.HistoricoService;
import com.IntegraSaude.service.LembreteService;
import com.IntegraSaude.service.SessaoUsuario;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Lembretes pendentes, confirmação de doses e histórico. */
public class MenuLembretes extends MenuBase {

    private final LembreteService lembretes;
    private final HistoricoService historico;
    private final SessaoUsuario sessao;
    private final SelecaoPaciente selecao;

    public MenuLembretes(LeitorConsole leitor, SaidaConsole saida, LembreteService lembretes,
                         HistoricoService historico, SessaoUsuario sessao, SelecaoPaciente selecao) {
        super(leitor, saida);
        this.lembretes = lembretes;
        this.historico = historico;
        this.sessao = sessao;
        this.selecao = selecao;
    }

    // Só o paciente vê a opção de confirmar; o responsável apenas acompanha
    public void executar() {
        executarMenu(() -> "Lembretes e historico", "Voltar", () -> {
            List<Opcao> opcoes = new ArrayList<>();
            opcoes.add(new Opcao("Ver lembretes pendentes", this::listarPendentes));
            if (sessao.exigirLogado().ehPaciente()) {
                opcoes.add(new Opcao("Confirmar dose/consulta", this::confirmar));
            }
            opcoes.add(new Opcao("Ver historico completo", this::historicoCompleto));
            opcoes.add(new Opcao("Ver historico por periodo", this::historicoPorPeriodo));
            return opcoes;
        });
    }

    private void listarPendentes() {
        selecao.escolher().ifPresent(paciente -> {
            List<Lembrete> lista = lembretes.listarPendentes(paciente.getId());
            if (lista.isEmpty()) {
                saida.exibir("Nenhum lembrete pendente.");
            }
            lista.forEach(l -> saida.exibir("[" + l.getId() + "] "
                    + l.getHorarioPrevisto().format(Formatos.DATA_HORA) + " - " + l.getDescricao()));
        });
    }

    private void confirmar() {
        listarPendentes();
        Long id = leitor.lerId("Codigo do lembrete a confirmar:");
        lembretes.confirmar(sessao.exigirLogado().getId(), id);
        saida.exibir("Confirmado! Registrado no historico.");
    }

    private void historicoCompleto() {
        selecao.escolher().ifPresent(paciente -> exibirHistorico(historico.listarDoPaciente(paciente.getId())));
    }

    // Período vai de 00:00 da data inicial até 23:59:59 da data final
    private void historicoPorPeriodo() {
        selecao.escolher().ifPresent(paciente -> {
            var de = leitor.lerData("Data inicial");
            var ate = leitor.lerData("Data final");
            exibirHistorico(historico.listarPorPeriodo(paciente.getId(),
                    de.atStartOfDay(), ate.atTime(LocalTime.MAX)));
        });
    }

    private void exibirHistorico(List<EventoHistorico> eventos) {
        if (eventos.isEmpty()) {
            saida.exibir("Nenhum evento no historico.");
        }
        eventos.forEach(e -> saida.exibir(e.getDataHora().format(Formatos.DATA_HORA)
                + " | " + e.getTipo() + " | " + e.getDescricao()));
    }
}
