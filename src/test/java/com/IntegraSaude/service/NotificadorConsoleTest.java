package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class NotificadorConsoleTest {

    @Test
    void deveExibirMensagemComDestinatarioEmDestaque() {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Notificador notificador = new NotificadorConsole(new PrintStream(saida, true, StandardCharsets.UTF_8));
        Usuario carlos = new Usuario(2L, "Carlos", "carlos@email.com", "hash", Perfil.RESPONSAVEL);

        notificador.notificar(carlos, "ALERTA: Ana Maria não confirmou o medicamento.");

        String texto = saida.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("Para: Carlos"), texto);
        assertTrue(texto.contains("ALERTA: Ana Maria não confirmou o medicamento."), texto);
        assertTrue(texto.contains("=========="), texto);
    }
}
