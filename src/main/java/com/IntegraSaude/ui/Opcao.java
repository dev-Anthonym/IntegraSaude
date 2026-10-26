package com.IntegraSaude.ui;

/** Uma linha de menu: o texto mostrado e o que acontece ao escolher. */
public record Opcao(String titulo, Runnable acao) {
}
