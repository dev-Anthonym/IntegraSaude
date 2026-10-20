package com.IntegraSaude.service;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// Serviço responsável pelas regras de negócio e fluxo de cadastro de usuários
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    // Injeção de dependência do repositório (facilita os testes unitários)
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Método principal para cadastrar um novo usuário no sistema
    public Usuario cadastrar(String nome, String email, String senhaPura, Perfil perfil) {
        
        // 1. Verifica se já existe um usuário cadastrado com este e-mail (RN01 - E-mail único)
        if (usuarioRepository.buscarPorEmail(email).isPresent()) {
            throw new RegistroDuplicadoException("Já existe um usuário cadastrado com o e-mail: " + email);
        }

        // 2. Cria o objeto temporário para validação das regras (RN01 a RN04)
        // Passamos uma senha temporária apenas para validar o formato dela
        Usuario usuarioTemp = new Usuario(null, nome, email, senhaPura, perfil);
        ValidadorUsuario.validar(usuarioTemp);

        // 3. Gera o hash seguro da senha para nunca guardá-la em texto puro (RN02)
        String senhaHash = hashSenha(senhaPura);

        // 4. Cria o usuário definitivo com a senha criptografada
        Usuario novoUsuario = new Usuario(null, nome, email, senhaHash, perfil);

        // 5. Salva no repositório e retorna o usuário cadastrado
        return usuarioRepository.salvar(novoUsuario);
    }

    // Método utilitário para gerar o hash SHA-256 da senha (conforme exigido na Etapa 2)
    public String hashSenha(String senhaPura) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(senhaPura.getBytes());
            return Base64.getEncoder().encodeToString(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash da senha", e);
        }
    }
}