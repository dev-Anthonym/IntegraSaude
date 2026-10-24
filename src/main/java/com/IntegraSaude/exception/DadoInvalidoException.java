package com.IntegraSaude.exception;

// Acionada quando o dado fornecido pelo usuário não atende às regras de negócio
public class DadoInvalidoException extends AppException {
    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }
}