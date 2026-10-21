package com.IntegraSaude.model;

// Classe que representa a entidade Usuário no nosso sistema
public class Usuario {
    private Long id;
    private String nome;
    private String email;
    private String senhaHash;
    private Perfil perfil; 
    private Long responsavelId;

    // Construtor completo para instanciar o utilizador
    public Usuario(Long id, String nome, String email, String senhaHash, Perfil perfil) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senhaHash = senhaHash;
        this.perfil = perfil;
    }

    // Métodos Getters e Setters para aceder e modificar os atributos com segurança
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }

    public Long getResponsavelId() { return responsavelId; }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

}