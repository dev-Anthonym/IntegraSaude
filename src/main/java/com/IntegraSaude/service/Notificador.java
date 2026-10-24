package com.IntegraSaude.service;

import com.IntegraSaude.model.Usuario;

// Canal de envio de avisos e alertas (console hoje; e-mail ou SMS no futuro)
public interface Notificador {
    void notificar(Usuario destinatario, String mensagem);
}
