package com.IntegraSaude.exception;

/** Falha em serviço externo (ex.: ViaCEP fora do ar ou sem rede). */
public class ServicoExternoException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public ServicoExternoException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public ServicoExternoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
