package com.IntegraSaude.exception;

/** Registro procurado não existe (id ou e-mail inexistente). */
public class RegistroNaoEncontradoException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public RegistroNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public RegistroNaoEncontradoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
