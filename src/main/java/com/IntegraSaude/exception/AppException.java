package com.IntegraSaude.exception;

/**
 * Exceção base da aplicação. Todas as exceções personalizadas herdam desta,
 * permitindo o tratamento unificado na camada de apresentação (UI).
 */
public class AppException extends RuntimeException {

    public AppException(String mensagem) {
        super(mensagem);
    }

    public AppException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}