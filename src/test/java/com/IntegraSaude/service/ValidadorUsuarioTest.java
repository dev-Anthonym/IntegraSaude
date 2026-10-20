package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Classe de testes unitários para validar as regras de negócio de cadastro (RN01 a RN04)
public class ValidadorUsuarioTest {

    @Test
    void deveAceitarDadosValidos() {
        // Cenário com dados perfeitamente válidos
        Usuario usuario = new Usuario(1L, "Maria Silva", "maria@email.com", "Senha123", Perfil.PACIENTE);
        
        // Valida que não deve lançar nenhuma exceção
        assertDoesNotThrow(() -> ValidadorUsuario.validar(usuario));
    }

    @Test
    void deveRejeitarEmailSemArroba() {
        // E-mail inválido por falta do '@' (RN01)
        Usuario usuario = new Usuario(1L, "Maria Silva", "mariaemail.com", "Senha123", Perfil.PACIENTE);
        
        // Garante que o sistema lança a nossa exceção de dado inválido
        assertThrows(DadoInvalidoException.class, () -> ValidadorUsuario.validar(usuario));
    }

    @Test
    void deveRejeitarEmailVazio() {
        // E-mail vazio ou nulo (RN01)
        Usuario usuario = new Usuario(1L, "Maria Silva", "", "Senha123", Perfil.PACIENTE);
        
        assertThrows(DadoInvalidoException.class, () -> ValidadorUsuario.validar(usuario));
    }

    @Test
    void deveRejeitarSenhaCurta() {
        // Senha com menos de 8 caracteres (RN02)
        Usuario usuario = new Usuario(1L, "Maria Silva", "maria@email.com", "Sen1", Perfil.PACIENTE);
        
        assertThrows(DadoInvalidoException.class, () -> ValidadorUsuario.validar(usuario));
    }

    @Test
    void deveRejeitarSenhaSemNumero() {
        // Senha sem conter números (RN02)
        Usuario usuario = new Usuario(1L, "Maria Silva", "maria@email.com", "SenhaSemNumero", Perfil.PACIENTE);
        
        assertThrows(DadoInvalidoException.class, () -> ValidadorUsuario.validar(usuario));
    }

    @Test
    void deveRejeitarNomeVazio() {
        // Nome obrigatório em branco (RN03)
        Usuario usuario = new Usuario(1L, "   ", "maria@email.com", "Senha123", Perfil.PACIENTE);
        
        assertThrows(DadoInvalidoException.class, () -> ValidadorUsuario.validar(usuario));
    }
}