package com.IntegraSaude.exception;

/** A entrada do console acabou (Ctrl+D ou fim do arquivo). Serve para encerrar o menu sem loop infinito. */
public class EntradaEncerradaException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public EntradaEncerradaException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public EntradaEncerradaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
