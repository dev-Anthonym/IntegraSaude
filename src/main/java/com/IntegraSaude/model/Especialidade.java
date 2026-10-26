package com.IntegraSaude.model;

/** Especialidade médica (ex.: Cardiologia). Separada do médico por causa da 3FN. */
public class Especialidade extends EntidadeBase {

    private final String nome;

    public Especialidade(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
