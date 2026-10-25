package com.IntegraSaude.exception;

/**
 * A classe ValidacaoException é uma exceção personalizada do tipo RuntimeException.
 * Ela é utilizada para interromper o fluxo e informar erros de validação de regras de negócio
 * (por exemplo, quando campos obrigatórios não são preenchidos).
 */
public class ValidacaoException extends RuntimeException {

    /**
     * Construtor que recebe a mensagem de erro detalhada.
     * @param mensagem Texto explicativo sobre o motivo do erro de validação
     */
    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}