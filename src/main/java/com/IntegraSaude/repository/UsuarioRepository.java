package com.IntegraSaude.repository;

import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import java.util.List;
import java.util.Optional;

/** Acesso a dados de usuários. Quem decide "não encontrado" é o serviço (Optional vazio aqui). */
public interface UsuarioRepository {

    // Insere o usuário e devolve o mesmo objeto já com id. E-mail repetido lança RegistroDuplicadoException
    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    Optional<Usuario> buscarPorEmail(String email);

    List<Usuario> listarPorPerfil(Perfil perfil);
}
