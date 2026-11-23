package com.IntegraSaude.web;

import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gerador de JSON mínimo, sem biblioteca externa. Aceita Map, Collection, String, número,
 * boolean, enum, datas do java.time e null. Só escreve: as requisições chegam como formulário.
 */
public final class Json {

    private static final int ULTIMO_CONTROLE = 0x1F;

    private Json() {
    }

    // Monta um objeto JSON preservando a ordem das chaves e aceitando valores nulos
    public static Map<String, Object> objeto(Object... chavesEValores) {
        if (chavesEValores.length % 2 != 0) {
            throw new IllegalArgumentException("Informe pares de chave e valor");
        }
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            mapa.put((String) chavesEValores[i], chavesEValores[i + 1]);
        }
        return mapa;
    }

    public static String escrever(Object valor) {
        StringBuilder texto = new StringBuilder();
        escrever(texto, valor);
        return texto.toString();
    }

    private static void escrever(StringBuilder saida, Object valor) {
        switch (valor) {
            case null -> saida.append("null");
            case String texto -> escreverTexto(saida, texto);
            case Number numero -> saida.append(numero);
            case Boolean logico -> saida.append(logico);
            case Enum<?> constante -> escreverTexto(saida, constante.name());
            case TemporalAccessor data -> escreverTexto(saida, data.toString());
            case Map<?, ?> mapa -> escreverObjeto(saida, mapa);
            case Collection<?> lista -> escreverLista(saida, lista);
            default -> throw new IllegalArgumentException("Tipo sem conversao para JSON: " + valor.getClass());
        }
    }

    private static void escreverObjeto(StringBuilder saida, Map<?, ?> mapa) {
        saida.append('{');
        boolean primeiro = true;
        for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
            if (!primeiro) {
                saida.append(',');
            }
            primeiro = false;
            escreverTexto(saida, String.valueOf(entrada.getKey()));
            saida.append(':');
            escrever(saida, entrada.getValue());
        }
        saida.append('}');
    }

    private static void escreverLista(StringBuilder saida, Collection<?> lista) {
        saida.append('[');
        boolean primeiro = true;
        for (Object item : lista) {
            if (!primeiro) {
                saida.append(',');
            }
            primeiro = false;
            escrever(saida, item);
        }
        saida.append(']');
    }

    // Escapa aspas, barra, controles e os caracteres que poderiam fechar um <script> no HTML
    private static void escreverTexto(StringBuilder saida, String texto) {
        saida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> saida.append("\\\"");
                case '\\' -> saida.append("\\\\");
                case '\n' -> saida.append("\\n");
                case '\r' -> saida.append("\\r");
                case '\t' -> saida.append("\\t");
                case '<', '>', '&', ' ', ' ' -> saida.append(String.format("\\u%04x", (int) c));
                default -> {
                    if (c <= ULTIMO_CONTROLE) {
                        saida.append(String.format("\\u%04x", (int) c));
                    } else {
                        saida.append(c);
                    }
                }
            }
        }
        saida.append('"');
    }
}
