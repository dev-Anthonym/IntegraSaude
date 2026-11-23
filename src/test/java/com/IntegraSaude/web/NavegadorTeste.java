package com.IntegraSaude.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Cliente HTTP de teste que se comporta como o navegador: guarda o cookie de sessão
 * e envia o cabeçalho X-Requested-With nos POST, como o app.js faz.
 */
final class NavegadorTeste {

    static final String SENHA = "Senha123";

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final int porta;
    String cookie = "";

    NavegadorTeste(ServidorWeb servidor) {
        this.porta = servidor.porta();
    }

    URI uri(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }

    HttpResponse<String> get(String caminho) throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder(uri(caminho)).GET());
    }

    HttpResponse<String> post(String caminho, String... chavesEValores) throws IOException, InterruptedException {
        return enviar(HttpRequest.newBuilder(uri(caminho))
                .header("X-Requested-With", "fetch")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formulario(chavesEValores))));
    }

    HttpResponse<String> enviar(HttpRequest.Builder requisicao) throws IOException, InterruptedException {
        if (!cookie.isEmpty()) {
            requisicao.header("Cookie", cookie);
        }
        HttpResponse<String> resposta = cliente.send(requisicao.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        resposta.headers().firstValue("Set-Cookie").ifPresent(valor -> cookie = valor.split(";", 2)[0]);
        return resposta;
    }

    NavegadorTeste cadastrar(String nome, String email, String perfil) throws Exception {
        HttpResponse<String> resposta = post("/api/cadastro", "nome", nome, "email", email, "senha", SENHA,
                "perfil", perfil);
        assertEquals(200, resposta.statusCode(), resposta.body());
        return this;
    }

    static String formulario(String... chavesEValores) {
        List<String> pares = new ArrayList<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            pares.add(URLEncoder.encode(chavesEValores[i], StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(chavesEValores[i + 1], StandardCharsets.UTF_8));
        }
        return String.join("&", pares);
    }
}
