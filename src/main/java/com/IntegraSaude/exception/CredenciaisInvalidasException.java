package com.IntegraSaude.exception;

/** Falha de login. A mensagem é sempre genérica para não revelar se errou e-mail ou senha (RN04). */
public class CredenciaisInvalidasException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public CredenciaisInvalidasException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
