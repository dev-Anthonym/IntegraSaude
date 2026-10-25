package com.IntegraSaude.exception;

/**
 * Exceção lançada quando ocorre uma operação ou transição de estado inválida.
 */
public class OperacaoInvalidaException extends AppException {
    
    public OperacaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    public OperacaoInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}