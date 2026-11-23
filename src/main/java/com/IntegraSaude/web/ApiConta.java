package com.IntegraSaude.web;

import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.Servicos;
import java.util.Locale;
import java.util.Map;

/** Rotas de conta: login, cadastro, logout e usuário da sessão. */
final class ApiConta {

    private final Servicos servicos;

    ApiConta(Servicos servicos) {
        this.servicos = servicos;
    }

    void registrar(ServidorWeb servidor) {
        servidor.post("/api/login", this::login);
        servidor.post("/api/cadastro", this::cadastrar);
        servidor.post("/api/logout", this::logout);
        servidor.get("/api/sessao", r -> usuario(r.exigirLogado()));
    }

    // Falha de login devolve a mensagem genérica do AutenticacaoService (RN04)
    private Object login(Requisicao requisicao) {
        Parametros p = requisicao.parametros();
        Usuario usuario = servicos.autenticacao().login(p.texto("email"), p.texto("senha"));
        requisicao.abrirSessao(usuario);
        return usuario(usuario);
    }

    // Cadastra e já entra na conta
    private Object cadastrar(Requisicao requisicao) {
        Parametros p = requisicao.parametros();
        Usuario usuario = servicos.usuarios().cadastrar(p.texto("nome"), p.texto("email"), p.texto("senha"),
                perfil(p.texto("perfil")));
        requisicao.abrirSessao(usuario);
        return usuario(usuario);
    }

    private Object logout(Requisicao requisicao) {
        requisicao.encerrarSessao();
        return null;
    }

    // Perfil desconhecido vira nulo, e o ValidadorUsuario devolve a mensagem certa (RN02)
    private static Perfil perfil(String texto) {
        try {
            return Perfil.valueOf(texto.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static Map<String, Object> usuario(Usuario usuario) {
        return Json.objeto("nome", usuario.getNome(), "primeiroNome", VisaoLembretes.primeiroNome(usuario.getNome()),
                "email", usuario.getEmail(), "perfil", usuario.getPerfil());
    }
}
