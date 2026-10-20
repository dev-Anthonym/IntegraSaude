package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Usuario;

// Classe responsável por validar as regras de negócio do usuário (RN01 a RN04)
public class ValidadorUsuario {

    // Método principal que recebe um usuário e valida todas as regras antes de prosseguir
    public static void validar(Usuario usuario) {
        if (usuario == null) {
            throw new DadoInvalidoException("O usuário não pode ser nulo.");
        }

        validarNome(usuario.getNome());       // Valida a RN03 (Nome obrigatório)
        validarEmail(usuario.getEmail());     // Valida a RN01 (E-mail válido)
        validarSenha(usuario.getSenhaHash()); // Valida a RN02 (Requisitos da senha)
    }

    // Regra RN03: O nome é obrigatório e não pode estar em branco
    private static void validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new DadoInvalidoException("O nome do usuário é obrigatório.");
        }
    }

    // Regra RN01: O e-mail deve ter formato válido (conter '@' e um domínio)
    private static void validarEmail(String email) {
        if (email == null || !email.contains("@") || email.trim().isEmpty()) {
            throw new DadoInvalidoException("E-mail com formato inválido.");
        }
    }

    // Regra RN02: A senha deve ter no mínimo 8 caracteres, contendo letra e número
    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < 8) {
            throw new DadoInvalidoException("A senha deve ter no mínimo 8 caracteres.");
        }

        boolean temLetra = false;
        boolean temNumero = false;

        // Varre cada caractere da senha para verificar se possui letras e números
        for (char c : senha.toCharArray()) {
            if (Character.isLetter(c)) {
                temLetra = true;
            } else if (Character.isDigit(c)) {
                temNumero = true;
            }
        }

        if (!temLetra || !temNumero) {
            throw new DadoInvalidoException("A senha deve conter letras e números.");
        }
    }
}