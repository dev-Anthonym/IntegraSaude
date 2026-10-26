package com.IntegraSaude.exception;

/** Tentativa de criar algo que já existe (e-mail, vínculo, consulta no mesmo horário...). */
public class RegistroDuplicadoException extends AppException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public RegistroDuplicadoException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa
    public RegistroDuplicadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
