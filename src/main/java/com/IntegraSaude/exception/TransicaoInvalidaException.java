package com.IntegraSaude.exception;

// Acionada quando um lembrete tenta mudar de status por uma transição não permitida
public class TransicaoInvalidaException extends AppException {
    public TransicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}