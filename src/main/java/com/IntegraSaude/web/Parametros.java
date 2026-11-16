package com.IntegraSaude.web;

import com.IntegraSaude.exception.DadoInvalidoException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Parâmetros de uma requisição (query string e corpo application/x-www-form-urlencoded).
 * Conversões inválidas viram DadoInvalidoException, que a API devolve como erro 400.
 */
public final class Parametros {

    private final Map<String, String> valores;

    private Parametros(Map<String, String> valores) {
        this.valores = valores;
    }

    // Interpreta "a=1&b=texto+com+espaco"; se uma chave se repetir, vale a última
    public static Parametros de(String... textosCodificados) {
        Map<String, String> mapa = new HashMap<>();
        for (String texto : textosCodificados) {
            if (texto == null || texto.isEmpty()) {
                continue;
            }
            for (String par : texto.split("&")) {
                if (par.isEmpty()) {
                    continue;
                }
                String[] partes = par.split("=", 2);
                mapa.put(decodificar(partes[0]), partes.length > 1 ? decodificar(partes[1]) : "");
            }
        }
        return new Parametros(mapa);
    }

    private static String decodificar(String texto) {
        try {
            return URLDecoder.decode(texto, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DadoInvalidoException("Parametro mal formado", e);
        }
    }

    // Texto sem espaços nas pontas; ausente vira vazio (os serviços validam obrigatoriedade)
    public String texto(String nome) {
        return opcional(nome).orElse("");
    }

    public Optional<String> opcional(String nome) {
        String valor = valores.get(nome);
        return valor == null || valor.isBlank() ? Optional.empty() : Optional.of(valor.trim());
    }

    public int inteiro(String nome, String rotulo) {
        try {
            return Integer.parseInt(texto(nome));
        } catch (NumberFormatException e) {
            throw new DadoInvalidoException("Informe um numero valido em " + rotulo, e);
        }
    }

    public Long id(String nome) {
        try {
            return Long.valueOf(texto(nome));
        } catch (NumberFormatException e) {
            throw new DadoInvalidoException("Identificador invalido", e);
        }
    }

    // Data no formato ISO (aaaa-mm-dd), que é o que o <input type="date"> envia
    public LocalDate data(String nome, String rotulo) {
        try {
            return LocalDate.parse(texto(nome));
        } catch (DateTimeParseException e) {
            throw new DadoInvalidoException("Informe uma data valida em " + rotulo, e);
        }
    }

    public Optional<LocalDate> dataOpcional(String nome) {
        return opcional(nome).isPresent() ? Optional.of(data(nome, nome)) : Optional.empty();
    }

    // Hora no formato HH:mm, que é o que o <input type="time"> envia
    public LocalTime hora(String nome, String rotulo) {
        try {
            return LocalTime.parse(texto(nome));
        } catch (DateTimeParseException e) {
            throw new DadoInvalidoException("Informe um horario valido em " + rotulo, e);
        }
    }
}
