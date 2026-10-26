package com.IntegraSaude.model;

/** Médico, sempre ligado a uma especialidade. */
public class Medico extends EntidadeBase {

    private final String nome;
    private final Especialidade especialidade;

    public Medico(String nome, Especialidade especialidade) {
        this.nome = nome;
        this.especialidade = especialidade;
    }

    public String getNome() {
        return nome;
    }

    public Especialidade getEspecialidade() {
        return especialidade;
    }
}
