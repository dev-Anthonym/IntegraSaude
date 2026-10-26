package com.IntegraSaude.service;

import com.IntegraSaude.model.Usuario;

/**
 * Canal de aviso ao usuário. Hoje há só a implementação de console (NotificadorConsole).
 * Para enviar e-mail no futuro, basta criar outra implementação.
 */
public interface Notificador {

    void notificar(Usuario destinatario, String mensagem);
}
