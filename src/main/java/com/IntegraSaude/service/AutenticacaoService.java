package com.IntegraSaude.service;

import com.IntegraSaude.exception.CredenciaisInvalidasException;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;

import java.util.Optional;

// Serviço responsável pela autenticação e gestão da sessão de utilizadores
public class AutenticacaoService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    
    // Atributo estático simples para guardar a sessão do utilizador logado em memória
    private static Usuario usuarioLogadoAtual = null;

    // Construtor para injeção de dependências
    public AutenticacaoService(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    // Método de login: valida e-mail e palavra-passe, retornando o utilizador ou lançando exceção (RN09)
    public Usuario login(String email, String senhaPura) {
        // 1. Procura o utilizador pelo e-mail
        Optional<Usuario> usuarioOpt = usuarioRepository.buscarPorEmail(email);

        // 2. Se o utilizador não existe OU se a palavra-passe não confere com o hash guardado, falha
        if (usuarioOpt.isEmpty()) {
            throw new CredenciaisInvalidasException("credenciais inválidas");
        }

        Usuario usuario = usuarioOpt.get();
        String senhaHashInformada = usuarioService.hashSenha(senhaPura);

        // Compara o hash gerado com o hash guardado na base de dados
        if (!usuario.getSenhaHash().equals(senhaHashInformada)) {
            throw new CredenciaisInvalidasException("credenciais inválidas");
        }

        // 3. Guarda o utilizador na sessão em memória
        usuarioLogadoAtual = usuario;
        return usuario;
    }

    // Método para consultar o utilizador atualmente logado (útil para os outros módulos)
    public static Usuario getUsuarioLogado() {
        return usuarioLogadoAtual;
    }

    // Método de logout para encerrar a sessão
    public static void logout() {
        usuarioLogadoAtual = null;
    }
}