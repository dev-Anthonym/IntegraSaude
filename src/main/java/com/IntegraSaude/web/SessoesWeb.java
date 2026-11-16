package com.IntegraSaude.web;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessões do navegador. No console existe um único usuário logado (SessaoUsuario); na web cada
 * navegador tem a sua, identificada por um token aleatório guardado em cookie HttpOnly.
 */
public class SessoesWeb {

    public static final Duration DURACAO = Duration.ofHours(12);
    private static final int TAMANHO_TOKEN_BYTES = 32;

    private record Sessao(Long usuarioId, Instant expiraEm) {
    }

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final SecureRandom aleatorio = new SecureRandom();
    private final Clock relogio;

    public SessoesWeb(Clock relogio) {
        this.relogio = relogio;
    }

    // Abre uma sessão e devolve o token que vai no cookie
    public String abrir(Long usuarioId) {
        byte[] bytes = new byte[TAMANHO_TOKEN_BYTES];
        aleatorio.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(usuarioId, relogio.instant().plus(DURACAO)));
        return token;
    }

    // Id do usuário dono do token, se a sessão existir e não tiver expirado
    public Optional<Long> usuarioDo(String token) {
        if (token == null) {
            return Optional.empty();
        }
        Sessao sessao = sessoes.get(token);
        if (sessao == null) {
            return Optional.empty();
        }
        if (relogio.instant().isAfter(sessao.expiraEm())) {
            sessoes.remove(token);
            return Optional.empty();
        }
        return Optional.of(sessao.usuarioId());
    }

    public void encerrar(String token) {
        if (token != null) {
            sessoes.remove(token);
        }
    }
}
