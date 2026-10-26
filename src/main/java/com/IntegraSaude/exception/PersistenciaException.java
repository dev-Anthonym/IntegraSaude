package com.IntegraSaude.exception;

/** Falha de banco de dados. A SQLException nunca deve chegar ao menu. */
public class PersistenciaException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public PersistenciaException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
