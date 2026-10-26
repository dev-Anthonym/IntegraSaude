package com.IntegraSaude.model;

/** Pessoa que usa o sistema. A senha nunca é guardada em texto puro: só o hash (RN01). */
public class Usuario extends EntidadeBase {

    private final String nome;
    private final String email;
    private final String senhaHash;
    private final Perfil perfil;

    public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    // Atalhos de leitura para deixar o código das regras mais legível
    public boolean ehPaciente() {
        return perfil == Perfil.PACIENTE;
    }

    public boolean ehResponsavel() {
        return perfil == Perfil.RESPONSAVEL;
    }
}
