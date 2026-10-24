package com.IntegraSaude.service;

import com.IntegraSaude.model.Usuario;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Notificador de teste que guarda as mensagens enviadas e pode simular falhas
class NotificadorFalso implements Notificador {

    record Envio(Usuario destinatario, String mensagem) {
    }

    private final List<Envio> envios = new ArrayList<>();
    private final Set<Long> destinatariosComFalha = new HashSet<>();

    @Override
    public void notificar(Usuario destinatario, String mensagem) {
        if (destinatariosComFalha.contains(destinatario.getId())) {
            throw new IllegalStateException("Falha simulada no envio");
        }
        envios.add(new Envio(destinatario, mensagem));
    }

    void falharPara(Usuario destinatario) {
        destinatariosComFalha.add(destinatario.getId());
    }

    List<Envio> envios() {
        return envios;
    }

    List<Usuario> destinatarios() {
        return envios.stream().map(Envio::destinatario).toList();
    }

    List<Envio> enviosPara(Usuario destinatario) {
        return envios.stream().filter(e -> e.destinatario().equals(destinatario)).toList();
    }
}
