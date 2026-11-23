package com.IntegraSaude.web;

import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.UsuarioService;
import com.sun.net.httpserver.HttpExchange;
import java.util.List;
import java.util.Optional;

/** Dados de uma chamada à API: parâmetros, cookie de sessão e o usuário logado (se houver). */
public class Requisicao {

    static final String COOKIE_SESSAO = "integra_sessao";

    private final HttpExchange troca;
    private final Parametros parametros;
    private final SessoesWeb sessoes;
    private final UsuarioService usuarios;

    Requisicao(HttpExchange troca, Parametros parametros, SessoesWeb sessoes, UsuarioService usuarios) {
        this.troca = troca;
        this.parametros = parametros;
        this.sessoes = sessoes;
        this.usuarios = usuarios;
    }

    public Parametros parametros() {
        return parametros;
    }

    public Optional<Usuario> usuarioLogado() {
        return sessoes.usuarioDo(token()).map(usuarios::buscarPorId);
    }

    public Usuario exigirLogado() {
        return usuarioLogado().orElseThrow(
                () -> new AcessoNegadoException(AcessoNegadoException.SEM_LOGIN, "E necessario fazer login"));
    }

    public Usuario exigirPaciente() {
        Usuario usuario = exigirLogado();
        if (!usuario.ehPaciente()) {
            throw new AcessoNegadoException(AcessoNegadoException.PROIBIDO, "Disponivel apenas para pacientes");
        }
        return usuario;
    }

    public Usuario exigirResponsavel() {
        Usuario usuario = exigirLogado();
        if (!usuario.ehResponsavel()) {
            throw new AcessoNegadoException(AcessoNegadoException.PROIBIDO,
                    "Disponivel apenas para responsaveis");
        }
        return usuario;
    }

    // Abre a sessão e grava o cookie (HttpOnly: o JavaScript da página não consegue ler)
    public void abrirSessao(Usuario usuario) {
        sessoes.encerrar(token());
        String token = sessoes.abrir(usuario.getId());
        gravarCookie(token, SessoesWeb.DURACAO.toSeconds());
    }

    public void encerrarSessao() {
        sessoes.encerrar(token());
        gravarCookie("", 0);
    }

    private void gravarCookie(String valor, long segundos) {
        troca.getResponseHeaders().add("Set-Cookie", COOKIE_SESSAO + "=" + valor
                + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + segundos);
    }

    // Lê o token do cabeçalho Cookie ("a=1; integra_sessao=xyz")
    private String token() {
        List<String> cabecalhos = troca.getRequestHeaders().getOrDefault("Cookie", List.of());
        for (String cabecalho : cabecalhos) {
            for (String cookie : cabecalho.split(";")) {
                String[] partes = cookie.trim().split("=", 2);
                if (partes.length == 2 && partes[0].equals(COOKIE_SESSAO) && !partes[1].isEmpty()) {
                    return partes[1];
                }
            }
        }
        return null;
    }
}
