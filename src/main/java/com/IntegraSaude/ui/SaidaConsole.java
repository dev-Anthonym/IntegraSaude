package com.IntegraSaude.ui;

import java.io.PrintStream;

/**
 * Único ponto de saída do console. Os métodos são synchronized porque o agendador
 * (outra thread) também escreve, e as mensagens não podem se misturar com o menu.
 */
public class SaidaConsole {

    private static final String MARCADOR_AVISO = ">>> ";

    private final PrintStream saida;

    public SaidaConsole(PrintStream saida) {
        this.saida = saida;
    }

    // Linha normal
    public synchronized void exibir(String texto) {
        saida.println(texto);
    }

    // Texto sem quebra de linha (usado nas perguntas)
    public synchronized void imprimir(String texto) {
        saida.print(texto);
        saida.flush();
    }

    // Aviso do agendador: linha em branco antes e marcador, para se destacar do menu
    public synchronized void exibirAviso(String texto) {
        saida.println();
        saida.println(MARCADOR_AVISO + texto);
        saida.flush();
    }
}
