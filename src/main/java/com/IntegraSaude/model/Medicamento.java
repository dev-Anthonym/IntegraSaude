package com.IntegraSaude.model;

/**
 * A classe Medicamento representa a entidade principal de domínio do sistema.
 * Ela atua como um "Model" (Modelo), guardando os dados de um medicamento cadastrado
 * para o paciente.
 */
public class Medicamento {

    // --- ATRIBUTOS (CAMPOS) ---
    // Representam as colunas que teremos na nossa tabela do banco de dados SQLite.
    private Long id;       // Identificador único do medicamento
    private String nome;   // Nome comercial ou genérico do remédio
    private String dosagem;// Quantidade ou concentração (ex: 500mg)
    private String horario;// Horário programado para a tomada (ex: 08:00)

    /**
     * Construtor Vazio (Default).
     * Necessário para instanciar objetos sem passar valores iniciais,
     * útil para frameworks ou quando preenchemos os dados via setters depois.
     */
    public Medicamento() {}

    /**
     * Construtor Completo com Parâmetros.
     * Permite criar um objeto Medicamento já com todos os dados preenchidos de uma vez.
     */
    public Medicamento(Long id, String nome, String dosagem, String horario) {
        this.id = id;
        this.nome = nome;
        this.dosagem = dosagem;
        this.horario = horario;
    }

    // --- GETTERS E SETTERS ---
    // Métodos de acesso para proteger e manipular os atributos privados da classe (Encapsulamento).

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDosagem() {
        return dosagem;
    }

    public void setDosagem(String dosagem) {
        this.dosagem = dosagem;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    /**
     * Método toString().
     * Sobrescreve o comportamento padrão do Java para retornar uma representação
     * em formato de texto legível (útil para exibir os dados no console ou logs).
     */
    @Override
    public String toString() {
        return "Medicamento [ID=" + id + ", Nome=" + nome + ", Dosagem=" + dosagem + ", Horário=" + horario + "]";
    }
}