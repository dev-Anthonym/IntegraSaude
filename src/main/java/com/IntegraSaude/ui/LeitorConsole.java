package com.IntegraSaude.ui;

import com.IntegraSaude.exception.EntradaEncerradaException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Concentra TODA a leitura de entrada. Cada método repete a pergunta se a entrada for
 * inválida, então uma digitação errada nunca derruba o programa.
 */
public class LeitorConsole {

    private final Scanner scanner;
    private final SaidaConsole saida;

    public LeitorConsole(Scanner scanner, SaidaConsole saida) {
        this.scanner = scanner;
        this.saida = saida;
    }

    // Texto obrigatório: repete enquanto vier vazio
    public String lerTexto(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta).trim();
            if (!texto.isEmpty()) {
                return texto;
            }
            saida.exibir("Este campo e obrigatorio.");
        }
    }

    // Texto opcional: ENTER devolve vazio
    public String lerTextoOpcional(String pergunta) {
        return lerLinha(pergunta).trim();
    }

    // Número inteiro dentro de um intervalo
    public int lerInteiro(String pergunta, int minimo, int maximo) {
        while (true) {
            String texto = lerLinha(pergunta).trim();
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) {
                    return valor;
                }
                saida.exibir("Digite um numero entre " + minimo + " e " + maximo + ".");
            } catch (NumberFormatException e) {
                saida.exibir("Entrada invalida. Digite apenas numeros.");
            }
        }
    }

    // Id (número longo positivo)
    public Long lerId(String pergunta) {
        return (long) lerInteiro(pergunta, 1, Integer.MAX_VALUE);
    }

    // Data no formato dd/MM/aaaa
    public LocalDate lerData(String pergunta) {
        while (true) {
            try {
                return LocalDate.parse(lerLinha(pergunta + " (dd/mm/aaaa)").trim(), Formatos.DATA);
            } catch (DateTimeParseException e) {
                saida.exibir("Data invalida. Use o formato dd/mm/aaaa, por exemplo 25/12/2026.");
            }
        }
    }

    // Data opcional: ENTER devolve vazio
    public Optional<LocalDate> lerDataOpcional(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta + " (dd/mm/aaaa, ENTER para hoje)").trim();
            if (texto.isEmpty()) {
                return Optional.empty();
            }
            try {
                return Optional.of(LocalDate.parse(texto, Formatos.DATA));
            } catch (DateTimeParseException e) {
                saida.exibir("Data invalida. Use o formato dd/mm/aaaa.");
            }
        }
    }

    // Hora no formato HH:mm
    public LocalTime lerHora(String pergunta) {
        while (true) {
            try {
                return LocalTime.parse(lerLinha(pergunta + " (HH:mm)").trim(), Formatos.HORA);
            } catch (DateTimeParseException e) {
                saida.exibir("Hora invalida. Use o formato HH:mm, por exemplo 08:30.");
            }
        }
    }

    // Pergunta de sim ou não
    public boolean confirmar(String pergunta) {
        while (true) {
            String texto = lerLinha(pergunta + " (s/n)").trim().toLowerCase();
            if (texto.equals("s") || texto.equals("sim")) {
                return true;
            }
            if (texto.equals("n") || texto.equals("nao")) {
                return false;
            }
            saida.exibir("Responda com s ou n.");
        }
    }

    // Mostra a pergunta e lê uma linha. Se a entrada acabou, encerra o menu de forma limpa
    private String lerLinha(String pergunta) {
        saida.imprimir(pergunta + " ");
        try {
            return scanner.nextLine();
        } catch (NoSuchElementException e) {
            throw new EntradaEncerradaException("Entrada encerrada", e);
        }
    }
}
