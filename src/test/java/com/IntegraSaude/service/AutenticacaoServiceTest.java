package com.IntegraSaude.service;

import com.IntegraSaude.exception.CredenciaisInvalidasException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Testes unitários para o serviço de autenticação (Etapa 3)
public class AutenticacaoServiceTest {

    private AutenticacaoService autenticacaoService;
    private UsuarioService usuarioService;
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryMemoria();
        usuarioService = new UsuarioService(usuarioRepository);
        autenticacaoService = new AutenticacaoService(usuarioRepository, usuarioService);
        
        // Cadastramos um utilizador válido de teste para usar nas validações de login
        usuarioService.cadastrar("Ana Maria", "ana@email.com", "Senha123", Perfil.PACIENTE);
    }

    @Test
    void deveAutenticarComCredenciaisCorretas() {
        // Tenta fazer login com o e-mail e a palavra-passe corretos
        Usuario usuarioLogado = autenticacaoService.login("ana@email.com", "Senha123");

        assertNotNull(usuarioLogado);
        assertEquals("ana@email.com", usuarioLogado.getEmail());
    }

    @Test
    void deveFalharComEmailInexistente() {
        // Tenta fazer login com um e-mail que não existe na base de dados
        CredenciaisInvalidasException excecao = assertThrows(
            CredenciaisInvalidasException.class, 
            () -> autenticacaoService.login("inexistente@email.com", "Senha123")
        );

        // Valida se a mensagem segue rigorosamente a RN09 (mensagem genérica)
        assertEquals("credenciais inválidas", excecao.getMessage());
    }

    @Test
    void deveFalharComSenhaIncorreta() {
        // Tenta fazer login com o e-mail certo mas com a palavra-passe errada
        CredenciaisInvalidasException excecao = assertThrows(
            CredenciaisInvalidasException.class, 
            () -> autenticacaoService.login("ana@email.com", "SenhaErrada1")
        );

        // Valida a regra RN09: a mensagem não pode revelar se foi o e-mail ou a senha
        assertEquals("credenciais inválidas", excecao.getMessage());
    }
}