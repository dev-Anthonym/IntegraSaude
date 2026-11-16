package com.IntegraSaude.web;

/** Uma rota da API: recebe a requisição e devolve o objeto que vira JSON (null vira {"ok":true}). */
@FunctionalInterface
public interface Rota {

    Object atender(Requisicao requisicao);
}
