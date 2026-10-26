package com.IntegraSaude.exception;

/** Operação não permitida no estado atual (ex.: confirmar lembrete que já foi perdido, RN09). */
public class OperacaoInvalidaException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public OperacaoInvalidaException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public OperacaoInvalidaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
