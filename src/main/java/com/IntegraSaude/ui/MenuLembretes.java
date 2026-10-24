package com.IntegraSaude.ui;

import com.IntegraSaude.exception.AppException;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.AutenticacaoService;
import com.IntegraSaude.service.GeradorLembretes;
import com.IntegraSaude.service.HistoricoService;
import com.IntegraSaude.service.LembreteService;
import java.io.PrintStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

// Opções do menu do módulo de lembretes: pendentes, confirmação e histórico
public class MenuLembretes {

    private static final Logger LOGGER = Logger.getLogger(MenuLembretes.class.getName());
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Scanner entrada;
    private final PrintStream saida;
    private final LembreteService lembreteService;
    private final HistoricoService historicoService;
    private final GeradorLembretes geradorLembretes;
    private final Clock relogio;

    public MenuLembretes(Scanner entrada, PrintStream saida, LembreteService lembreteService,
                         HistoricoService historicoService, GeradorLembretes geradorLembretes, Clock relogio) {
        this.entrada = entrada;
        this.saida = saida;
        this.lembreteService = lembreteService;
        this.historicoService = historicoService;
        this.geradorLembretes = geradorLembretes;
        this.relogio = relogio;
    }

    public void verPendentes() {
        executar("listar lembretes", () -> {
            List<Lembrete> pendentes = gerarEListarPendentes(identificarPaciente());
            saida.println("\n--- Lembretes pendentes ---");
            if (pendentes.isEmpty()) {
                saida.println("Nenhum lembrete pendente.");
            }
            pendentes.forEach(lembrete -> saida.println(formatar(lembrete)));
        });
    }

    public void confirmarLembrete() {
        executar("confirmar lembrete", () -> {
            List<Lembrete> pendentes = gerarEListarPendentes(identificarPaciente());
            pendentes.forEach(lembrete -> saida.println(formatar(lembrete)));
            saida.print("Número do lembrete a confirmar: ");
            Lembrete lembrete = buscarEntrePendentes(pendentes, lerNumero());
            lembreteService.confirmar(lembrete.getId());
            saida.println("\nLembrete #" + lembrete.getId() + " confirmado: " + respostaDeConfirmacao(lembrete)
                    + " (" + lembrete.getDescricao() + ").");
        });
    }

    public void verHistorico() {
        executar("exibir histórico", () -> {
            List<EventoHistorico> eventos = historicoService.listarDoPaciente(identificarPaciente());
            saida.println("\n--- Histórico de lembretes ---");
            if (eventos.isEmpty()) {
                saida.println("Nenhum evento registrado.");
            }
            eventos.forEach(evento -> saida.println(formatar(evento)));
        });
    }

    // Ponto único de tratamento de erros: mensagem clara para o usuário e detalhes técnicos só no log
    private void executar(String acao, Runnable comando) {
        try {
            comando.run();
        } catch (AppException e) {
            saida.println("\nErro: " + e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.warning("Falha inesperada ao " + acao + ": " + e.getMessage());
            LOGGER.log(Level.FINE, "Detalhes da falha ao " + acao, e);
            saida.println("\nErro inesperado. Tente novamente.");
        }
    }

    private List<Lembrete> gerarEListarPendentes(Long pacienteId) {
        geradorLembretes.gerarDoDia(pacienteId, LocalDate.now(relogio));
        return lembreteService.listarPendentes(pacienteId);
    }

    // Usa o paciente logado; sem login de paciente, pergunta o ID
    private Long identificarPaciente() {
        Usuario logado = AutenticacaoService.getUsuarioLogado();
        if (logado != null && logado.getPerfil() == Perfil.PACIENTE) {
            return logado.getId();
        }
        saida.print("ID do paciente: ");
        return lerNumero();
    }

    private Long lerNumero() {
        String digitado = entrada.hasNextLine() ? entrada.nextLine().trim() : "";
        try {
            return Long.parseLong(digitado);
        } catch (NumberFormatException e) {
            throw new DadoInvalidoException("Digite um número válido.");
        }
    }

    private Lembrete buscarEntrePendentes(List<Lembrete> pendentes, Long lembreteId) {
        return pendentes.stream()
                .filter(lembrete -> lembrete.getId().equals(lembreteId))
                .findFirst()
                .orElseThrow(() -> new RegistroNaoEncontradoException(
                        "Lembrete #" + lembreteId + " não encontrado entre os pendentes deste paciente."));
    }

    private String respostaDeConfirmacao(Lembrete lembrete) {
        return lembrete.getTipo() == TipoLembrete.MEDICAMENTO ? "tomei" : "compareci";
    }

    private String formatar(Lembrete lembrete) {
        String tipo = lembrete.getTipo() == TipoLembrete.MEDICAMENTO ? "Medicamento" : "Consulta";
        return "#" + lembrete.getId() + " | " + lembrete.getHorarioPrevisto().format(FORMATO_DATA_HORA)
                + " | " + tipo + " | " + lembrete.getDescricao();
    }

    private String formatar(EventoHistorico evento) {
        String descricao = lembreteService.buscarPorId(evento.getLembreteId()).getDescricao();
        return evento.getDataHora().format(FORMATO_DATA_HORA) + " | " + evento.getTipoEvento()
                + " | " + descricao + " (lembrete #" + evento.getLembreteId() + ")";
    }
}
