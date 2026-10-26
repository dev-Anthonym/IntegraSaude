package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.CredenciaisInvalidasException;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import org.junit.jupiter.api.Test;

/** RN01 a RN04: cadastro, hash de senha, login e vínculo. */
class UsuarioLoginVinculoTest {

    private final CenarioTeste c = new CenarioTeste();

    @Test
    void deveCadastrarUsuarioValido() {
        Usuario usuario = c.criarPaciente("ana@email.com");
        assertNotNull(usuario.getId());
        assertEquals(Perfil.PACIENTE, usuario.getPerfil());
    }

    @Test
    void deveRejeitarEmailDuplicado() {
        c.criarPaciente("ana@email.com");
        assertThrows(RegistroDuplicadoException.class, () -> c.criarResponsavel("ANA@email.com"));
    }

    @Test
    void deveGuardarSenhaComHash() {
        Usuario usuario = c.criarPaciente("ana@email.com");
        assertNotEquals(CenarioTeste.SENHA_VALIDA, usuario.getSenhaHash());
        assertFalse(usuario.getSenhaHash().contains(CenarioTeste.SENHA_VALIDA));
        assertTrue(c.senhas.conferir(CenarioTeste.SENHA_VALIDA, usuario.getSenhaHash()));
    }

    @Test
    void deveGerarHashDiferenteParaMesmaSenha() {
        assertNotEquals(c.senhas.gerarHash("Senha123"), c.senhas.gerarHash("Senha123"));
    }

    @Test
    void deveAutenticarComCredenciaisCorretas() {
        Usuario criado = c.criarPaciente("ana@email.com");
        Usuario logado = c.autenticacao.login("ana@email.com", CenarioTeste.SENHA_VALIDA);
        assertEquals(criado.getId(), logado.getId());
    }

    @Test
    void deveFalharComSenhaIncorreta() {
        c.criarPaciente("ana@email.com");
        assertThrows(CredenciaisInvalidasException.class,
                () -> c.autenticacao.login("ana@email.com", "SenhaErrada9"));
    }

    @Test
    void deveUsarMensagemGenericaNaFalha() {
        c.criarPaciente("ana@email.com");
        String mensagemSenhaErrada = assertThrows(CredenciaisInvalidasException.class,
                () -> c.autenticacao.login("ana@email.com", "SenhaErrada9")).getMessage();
        String mensagemEmailInexistente = assertThrows(CredenciaisInvalidasException.class,
                () -> c.autenticacao.login("naoexiste@email.com", "Senha123")).getMessage();
        assertEquals(mensagemSenhaErrada, mensagemEmailInexistente);
    }

    @Test
    void deveCriarVinculoEntrePacienteEResponsavel() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Usuario responsavel = c.criarResponsavel("r@email.com");
        c.vinculos.criar(paciente.getId(), responsavel.getId());
        assertEquals(1, c.vinculos.listarResponsaveisDoPaciente(paciente.getId()).size());
        assertEquals(1, c.vinculos.listarPacientesDoResponsavel(responsavel.getId()).size());
    }

    @Test
    void deveRejeitarVinculoComMesmoUsuario() {
        Usuario paciente = c.criarPaciente("p@email.com");
        assertThrows(DadoInvalidoException.class, () -> c.vinculos.criar(paciente.getId(), paciente.getId()));
    }

    @Test
    void deveRejeitarVinculoComPerfisErrados() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Usuario outroPaciente = c.criarPaciente("p2@email.com");
        assertThrows(DadoInvalidoException.class,
                () -> c.vinculos.criar(paciente.getId(), outroPaciente.getId()));
    }

    @Test
    void deveRejeitarVinculoDuplicado() {
        Usuario paciente = c.criarPaciente("p@email.com");
        Usuario responsavel = c.criarResponsavel("r@email.com");
        c.vinculos.criar(paciente.getId(), responsavel.getId());
        assertThrows(RegistroDuplicadoException.class,
                () -> c.vinculos.criar(paciente.getId(), responsavel.getId()));
    }

    @Test
    void devePermitirVariosResponsaveisParaUmPaciente() {
        Usuario paciente = c.criarPaciente("p@email.com");
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r1@email.com").getId());
        c.vinculos.criar(paciente.getId(), c.criarResponsavel("r2@email.com").getId());
        assertEquals(2, c.vinculos.listarResponsaveisDoPaciente(paciente.getId()).size());
    }
}
