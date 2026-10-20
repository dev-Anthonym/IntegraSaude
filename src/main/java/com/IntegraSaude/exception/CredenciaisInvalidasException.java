package com.IntegraSaude.exception;

// Acionada quando a autenticação falha por e-mail inexistente ou senha incorreta
public class CredenciaisInvalidasException extends AppException {
    public CredenciaisInvalidasException(String mensagem) {
        super(mensagem);
    }
}