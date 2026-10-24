package com.IntegraSaude.repository;

import java.sql.Connection;
import java.sql.SQLException;

// Fornece uma conexão nova a cada chamada; quem usa é responsável por fechá-la
@FunctionalInterface
public interface FabricaConexao {
    Connection abrir() throws SQLException;
}
