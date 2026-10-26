package com.IntegraSaude.integracao;

import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.exception.ServicoExternoException;
import com.IntegraSaude.model.Endereco;
import com.IntegraSaude.service.EnderecoClient;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente da API pública ViaCEP, com timeout curto para nunca travar o cadastro de consulta. */
public class ViaCepClient implements EnderecoClient {

    private static final String URL_BASE = "https://viacep.com.br/ws/";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final int STATUS_OK = 200;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    @Override
    public Endereco buscar(String cep) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(URL_BASE + cep + "/json/"))
                .timeout(TIMEOUT).GET().build();
        String corpo = enviar(requisicao);
        // Quando o CEP não existe, a API responde 200 com {"erro": "true"}
        if (corpo.contains("\"erro\"")) {
            throw new RegistroNaoEncontradoException("CEP nao encontrado");
        }
        return new Endereco(cep, campo(corpo, "logradouro"), campo(corpo, "bairro"),
                campo(corpo, "localidade"), campo(corpo, "uf"));
    }

    // Faz a chamada HTTP e transforma qualquer falha de rede em ServicoExternoException
    private String enviar(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            if (resposta.statusCode() != STATUS_OK) {
                throw new ServicoExternoException("O servico de CEP respondeu com erro " + resposta.statusCode());
            }
            return resposta.body();
        } catch (IOException e) {
            throw new ServicoExternoException("Nao foi possivel consultar o servico de CEP", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicoExternoException("A consulta de CEP foi interrompida", e);
        }
    }

    // Extrai o valor de um campo string do JSON simples da ViaCEP (sem biblioteca externa)
    private String campo(String json, String nome) {
        Matcher m = Pattern.compile("\"" + nome + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : "";
    }
}
