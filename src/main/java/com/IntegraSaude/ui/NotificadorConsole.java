package com.IntegraSaude.ui;

import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.Notificador;

/** Notificador que mostra o aviso no console. */
public class NotificadorConsole implements Notificador {

    private final SaidaConsole saida;

    public NotificadorConsole(SaidaConsole saida) {
        this.saida = saida;
    }

    @Override
    public void notificar(Usuario destinatario, String mensagem) {
        saida.exibirAviso("[para " + destinatario.getNome() + "] " + mensagem);
    }
}
