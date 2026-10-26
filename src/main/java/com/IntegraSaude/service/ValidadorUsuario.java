package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Perfil;
import java.util.regex.Pattern;

/** Valida os dados de cadastro de usuário (RN01 e RN02). */
public class ValidadorUsuario {

    // Constante do limite (nada de número solto no código)
    public static final int TAMANHO_MINIMO_SENHA = 8;

    // Formato simples de e-mail: texto@dominio.ext
    private static final Pattern PADRAO_EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)+$");

    // Roda todas as validações; a primeira que falhar lança DadoInvalidoException
    public void validar(String nome, String email, String senha, Perfil perfil) {
        validarNome(nome);
        validarEmail(email);
        validarSenha(senha);
        validarPerfil(perfil);
    }

    // Nome obrigatório
    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome e obrigatorio");
        }
    }

    // E-mail obrigatório e com formato válido
    private void validarEmail(String email) {
        if (email == null || !PADRAO_EMAIL.matcher(email.trim()).matches()) {
            throw new DadoInvalidoException("E-mail invalido");
        }
    }

    // Senha com mínimo de caracteres, pelo menos uma letra e um número
    private void validarSenha(String senha) {
        if (senha == null || senha.length() < TAMANHO_MINIMO_SENHA) {
            throw new DadoInvalidoException(
                    "A senha deve ter no minimo " + TAMANHO_MINIMO_SENHA + " caracteres");
        }
        boolean temLetra = senha.chars().anyMatch(Character::isLetter);
        boolean temNumero = senha.chars().anyMatch(Character::isDigit);
        if (!temLetra || !temNumero) {
            throw new DadoInvalidoException("A senha deve ter pelo menos uma letra e um numero");
        }
    }

    // Perfil obrigatório (o enum já garante que só existem PACIENTE e RESPONSAVEL)
    private void validarPerfil(Perfil perfil) {
        if (perfil == null) {
            throw new DadoInvalidoException("O perfil e obrigatorio (PACIENTE ou RESPONSAVEL)");
        }
    }
}
