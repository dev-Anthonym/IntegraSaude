package com.IntegraSaude;

import com.IntegraSaude.config.ConfiguracaoApp;
import com.IntegraSaude.exception.AppException;

/** Ponto de entrada. Só lê a configuração e delega para a Aplicacao. */
public final class Main {

    private static final int CODIGO_ERRO = 1;

    private Main() {
    }

    public static void main(String[] args) {
        try {
            Aplicacao.executar(ConfiguracaoApp.carregar(args));
        } catch (AppException e) {
            // Erro de configuração ou de banco: mostra só a mensagem, sem stack trace
            System.err.println("Erro: " + e.getMessage());
            System.exit(CODIGO_ERRO);
        }
    }
}
