package com.IntegraSaude.exception;

// Exceção base da aplicação (estende RuntimeException para evitar obrigatoriedade de try/catch excessivos)
public class AppException extends RuntimeException {
    public AppException(String mensagem) {
        super(mensagem);
    }
}