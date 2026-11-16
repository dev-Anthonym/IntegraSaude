package com.IntegraSaude.web;

/** Falta de login (401) ou perfil sem permissão para a rota (403). Exclusiva da camada web. */
public class AcessoNegadoException extends RuntimeException {

    public static final int SEM_LOGIN = 401;
    public static final int PROIBIDO = 403;

    private final int status;

    public AcessoNegadoException(int status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
