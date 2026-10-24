package com.IntegraSaude.service;

import com.IntegraSaude.model.Usuario;
import java.io.PrintStream;

// Exibe avisos e alertas no console com linhas de destaque, sem misturar com o menu
public class NotificadorConsole implements Notificador {

    private static final String LINHA_DESTAQUE = "=".repeat(60);

    private final PrintStream saida;

    public NotificadorConsole(PrintStream saida) {
        this.saida = saida;
    }

    @Override
    public void notificar(Usuario destinatario, String mensagem) {
        synchronized (saida) {
            saida.println();
            saida.println(LINHA_DESTAQUE);
            saida.println("Para: " + destinatario.getNome());
            saida.println(mensagem);
            saida.println(LINHA_DESTAQUE);
        }
    }
}
