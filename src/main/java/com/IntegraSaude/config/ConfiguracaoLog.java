package com.IntegraSaude.config;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/** Manda os logs para um arquivo, para o usuário nunca ver stack trace no console. */
public final class ConfiguracaoLog {

    private static final String ARQUIVO_LOG = "lembretes.log";

    private ConfiguracaoLog() {
    }

    public static void iniciar() {
        Logger raiz = Logger.getLogger("");
        // Remove o handler de console padrão (que imprimiria o stack trace na tela)
        for (Handler handler : raiz.getHandlers()) {
            raiz.removeHandler(handler);
        }
        try {
            FileHandler arquivo = new FileHandler(ARQUIVO_LOG, true);
            arquivo.setFormatter(new SimpleFormatter());
            raiz.addHandler(arquivo);
            raiz.setLevel(Level.INFO);
        } catch (IOException e) {
            // Sem arquivo de log a aplicação continua; só avisa uma vez em linha curta
            System.err.println("Aviso: nao foi possivel criar o arquivo de log " + ARQUIVO_LOG);
        }
    }
}
