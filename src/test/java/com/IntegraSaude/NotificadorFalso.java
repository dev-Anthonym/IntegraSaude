package com.IntegraSaude;

import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.Notificador;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Notificador de teste: guarda as mensagens e pode simular falha para certos destinatários. */
public class NotificadorFalso implements Notificador {

    public final List<String> mensagens = new ArrayList<>();
    public final Set<Long> destinatariosComFalha = new HashSet<>();

    @Override
    public void notificar(Usuario destinatario, String mensagem) {
        if (destinatariosComFalha.contains(destinatario.getId())) {
            throw new IllegalStateException("falha simulada");
        }
        mensagens.add(destinatario.getNome() + ": " + mensagem);
    }
}
