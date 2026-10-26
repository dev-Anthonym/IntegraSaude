package com.IntegraSaude.repository.jdbc;

import com.IntegraSaude.repository.Repositorios;
import java.sql.Connection;

/** Fábrica que monta o pacote de repositórios usando SQLite (no lugar das versões em memória). */
public final class RepositoriosJdbc {

    private RepositoriosJdbc() {
    }

    public static Repositorios criar(Connection conexao) {
        return new Repositorios(
                new UsuarioRepositoryJdbc(conexao),
                new VinculoRepositoryJdbc(conexao),
                new MedicamentoRepositoryJdbc(conexao),
                new EspecialidadeRepositoryJdbc(conexao),
                new MedicoRepositoryJdbc(conexao),
                new ConsultaRepositoryJdbc(conexao),
                new LembreteRepositoryJdbc(conexao),
                new HistoricoRepositoryJdbc(conexao));
    }
}
