package com.IntegraSaude.model;

/**
 * Classe base de todas as entidades que têm id.
 * O id é Long e fica nulo até a entidade ser salva (quem gera o valor é o repositório/banco).
 * equals e hashCode são feitos pelo id.
 */
public abstract class EntidadeBase {

    private Long id;

    // Devolve o id (nulo se ainda não foi salva)
    public Long getId() {
        return id;
    }

    // Chamado pelo repositório depois de salvar
    public void setId(Long id) {
        this.id = id;
    }

    // Duas entidades são iguais se forem da mesma classe e tiverem o mesmo id.
    // Entidade sem id (ainda não salva) só é igual a ela mesma.
    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (outro == null || getClass() != outro.getClass() || id == null) {
            return false;
        }
        return id.equals(((EntidadeBase) outro).id);
    }

    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }
}
