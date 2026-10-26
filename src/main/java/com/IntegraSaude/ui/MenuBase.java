package com.IntegraSaude.ui;

import com.IntegraSaude.exception.AppException;
import com.IntegraSaude.exception.EntradaEncerradaException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base de todos os menus. Aqui fica o laço do menu e o ÚNICO lugar que captura
 * AppException para mostrar a mensagem ao usuário, sem stack trace.
 */
abstract class MenuBase {

    private static final Logger LOGGER = Logger.getLogger(MenuBase.class.getName());
    private static final int OPCAO_SAIR = 0;

    protected final LeitorConsole leitor;
    protected final SaidaConsole saida;

    protected MenuBase(LeitorConsole leitor, SaidaConsole saida) {
        this.leitor = leitor;
        this.saida = saida;
    }

    // Laço genérico: mostra as opções, lê a escolha e executa. As opções são recalculadas a cada volta
    protected void executarMenu(Supplier<String> titulo, String rotuloSaida, Supplier<List<Opcao>> opcoes) {
        while (true) {
            List<Opcao> disponiveis = opcoes.get();
            exibirOpcoes(titulo.get(), rotuloSaida, disponiveis);
            int escolha = leitor.lerInteiro("Opcao:", OPCAO_SAIR, disponiveis.size());
            if (escolha == OPCAO_SAIR) {
                return;
            }
            executarSeguro(disponiveis.get(escolha - 1).acao());
        }
    }

    private void exibirOpcoes(String titulo, String rotuloSaida, List<Opcao> opcoes) {
        saida.exibir("");
        saida.exibir("=== " + titulo + " ===");
        for (int i = 0; i < opcoes.size(); i++) {
            saida.exibir((i + 1) + " - " + opcoes.get(i).titulo());
        }
        saida.exibir(OPCAO_SAIR + " - " + rotuloSaida);
    }

    // Executa uma ação tratando os erros: nenhuma entrada ou falha derruba o programa
    protected void executarSeguro(Runnable acao) {
        try {
            acao.run();
        } catch (EntradaEncerradaException e) {
            throw e;
        } catch (AppException e) {
            saida.exibir("Erro: " + e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Erro inesperado no menu", e);
            saida.exibir("Ocorreu um erro inesperado. Os detalhes foram registrados no log.");
        }
    }
}
