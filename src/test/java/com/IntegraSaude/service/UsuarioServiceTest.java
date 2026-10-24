package com.IntegraSaude.service;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Classe de testes unitários para o UsuarioService
public class UsuarioServiceTest {

    private UsuarioService usuarioService;
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        // Inicializa um repositório limpo em memória e o serviço antes de cada teste
        usuarioRepository = new UsuarioRepositoryMemoria();
        usuarioService = new UsuarioService(usuarioRepository);
    }

    @Test
    void deveCadastrarUsuarioValidoComSucesso() {
        // Tenta cadastrar um usuário válido
        Usuario usuarioCadastrado = usuarioService.cadastrar("Carlos Souza", "carlos@email.com", "Senha123", Perfil.RESPONSAVEL);

        // Valida se foi salvo gerando um ID e se a senha foi guardada com hash (e não em texto puro)
        assertNotNull(usuarioCadastrado.getId());
        assertEquals("carlos@email.com", usuarioCadastrado.getEmail());
        assertNotEquals("Senha123", usuarioCadastrado.getSenhaHash()); // Garante a RN02
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        // Cadastra o primeiro usuário
        usuarioService.cadastrar("Ana Paula", "ana@email.com", "Senha123", Perfil.PACIENTE);

        // Tenta cadastrar outro usuário com o mesmo e-mail (RN01)
        assertThrows(RegistroDuplicadoException.class, () -> {
            usuarioService.cadastrar("Outra Ana", "ana@email.com", "OutraSenha123", Perfil.PACIENTE);
        });
    }

    @Test
    void deveGerarHashSeguroParaSenha() {
        String senhaPura = "Segura123";
        String hash1 = usuarioService.hashSenha(senhaPura);
        String hash2 = usuarioService.hashSenha(senhaPura);

        // O hash gerado não pode ser igual à senha pura e deve ser consistente
        assertNotEquals(senhaPura, hash1);
        assertEquals(hash1, hash2);
    }
}