package com.IntegraSaude.exception;

// Acionada quando uma operação no banco de dados falha
public class PersistenciaException extends AppException {
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem);
        initCause(causa);
    }
}
