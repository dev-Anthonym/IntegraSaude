package com.IntegraSaude.exception;

// Acionada quando tentamos cadastrar um registro que já existe (ex: e-mail já cadastrado)
public class RegistroDuplicadoException extends AppException {
    public RegistroDuplicadoException(String mensagem) {
        super(mensagem);
    }
}