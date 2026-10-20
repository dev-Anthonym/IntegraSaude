package com.IntegraSaude.exception;

// Acionada quando um registro buscado (ex: ID ou e-mail) não é encontrado no sistema
public class RegistroNaoEncontradoException extends AppException {
    public RegistroNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}