package com.IntegraSaude.exception;

/**
 * Exceção base de toda a aplicação.
 * Herda de RuntimeException (não verificada) para não poluir assinaturas com "throws".
 * Quem trata as exceções é a camada ui.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Construtor só com mensagem
    public AppException(String mensagem) {
        super(mensagem);
    }

    // Construtor com mensagem e causa (preserva o erro original, por exemplo uma SQLException)
    public AppException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
