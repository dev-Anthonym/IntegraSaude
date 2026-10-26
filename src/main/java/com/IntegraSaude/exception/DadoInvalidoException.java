package com.IntegraSaude.exception;

/** Dado de entrada que viola uma regra de validação (RN01, RN05, RN08...). */
public class DadoInvalidoException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public DadoInvalidoException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public DadoInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
