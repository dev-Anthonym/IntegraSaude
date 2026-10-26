package com.IntegraSaude.model;

/** Endereço devolvido pela consulta de CEP (etapa opcional do ViaCEP). */
public record Endereco(String cep, String logradouro, String bairro, String cidade, String uf) {

    // Monta o endereço em uma linha só, para usar como "local" da consulta
    public String formatar() {
        return logradouro + ", " + bairro + " - " + cidade + "/" + uf;
    }
}
