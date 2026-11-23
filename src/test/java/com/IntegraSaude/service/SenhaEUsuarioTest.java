package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.exception.CredenciaisInvalidasException;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import org.junit.jupiter.api.Test;

/** Casos de erro e limites de senha, cadastro, login e vínculo. */
class SenhaEUsuarioTest {

    private final CenarioTeste cenario = new CenarioTeste();

    @Test
    void conferirDeveRecusarEntradasAusentesOuSemSeparador() {
        SenhaService senhas = new SenhaService();
        String hash = senhas.gerarHash("Senha123");
        assertTrue(senhas.conferir("Senha123", hash));
        assertFalse(senhas.conferir("senha123", hash), "diferencia maiusculas");
        assertFalse(senhas.conferir(null, hash));
        assertFalse(senhas.conferir("Senha123", null));
        assertFalse(senhas.conferir("Senha123", "texto-sem-separador"));
    }

    @Test
    void senhaComExatamente8CaracteresEValidaE7Nao() {
        ValidadorUsuario validador = new ValidadorUsuario();
        validador.validar("Ana", "ana@teste.com", "abcdefg1", Perfil.PACIENTE);
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar("Ana", "ana@teste.com", "abcdef1", Perfil.PACIENTE));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar("Ana", "ana@teste.com", "12345678", Perfil.PACIENTE), "sem letra");
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar("Ana", "ana@teste.com", null, Perfil.PACIENTE));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar("Ana", null, "abcdefg1", Perfil.PACIENTE));
        assertThrows(DadoInvalidoException.class,
                () -> validador.validar("Ana", "ana@teste", "abcdefg1", Perfil.PACIENTE), "sem dominio");
    }

    @Test
    void cadastroDeveNormalizarNomeEEmail() {
        Usuario ana = cenario.usuarios.cadastrar("  Ana Lima ", " Ana@Teste.COM ", "Senha123", Perfil.PACIENTE);
        assertEquals("Ana Lima", ana.getNome());
        assertEquals("ana@teste.com", ana.getEmail());
        assertEquals(ana, cenario.usuarios.buscarPorEmail("ANA@teste.com"));
        assertNull(UsuarioService.normalizarEmail(null));
    }

    @Test
    void loginComCamposVaziosDeveFalharComMensagemGenerica() {
        cenario.criarPaciente("ana@teste.com");
        CredenciaisInvalidasException erro = assertThrows(CredenciaisInvalidasException.class,
                () -> cenario.autenticacao.login("", ""));
        assertEquals(AutenticacaoService.MENSAGEM_FALHA, erro.getMessage());
        assertThrows(CredenciaisInvalidasException.class, () -> cenario.autenticacao.login(null, null));
    }

    @Test
    void buscasDeUsuarioInexistenteDevemFalhar() {
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.usuarios.buscarPorId(99L));
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.usuarios.buscarPorEmail("x@y.com"));
    }

    @Test
    void listarPacientesNaoIncluiResponsaveis() {
        cenario.criarPaciente("a@teste.com");
        cenario.criarResponsavel("b@teste.com");
        cenario.criarPaciente("c@teste.com");
        assertEquals(2, cenario.usuarios.listarPacientes().size());
    }

    @Test
    void vinculoDeveValidarExistenciaEPerfis() {
        Usuario paciente = cenario.criarPaciente("a@teste.com");
        Usuario responsavel = cenario.criarResponsavel("b@teste.com");
        assertThrows(RegistroNaoEncontradoException.class, () -> cenario.vinculos.criar(paciente.getId(), 99L));
        assertThrows(RegistroNaoEncontradoException.class,
                () -> cenario.vinculos.remover(paciente.getId(), responsavel.getId()));
        // Ordem trocada: responsável no lugar do paciente
        assertThrows(DadoInvalidoException.class,
                () -> cenario.vinculos.criar(responsavel.getId(), paciente.getId()));

        cenario.vinculos.criar(paciente.getId(), responsavel.getId());
        assertEquals(1, cenario.vinculos.listarPacientesDoResponsavel(responsavel.getId()).size());
        cenario.vinculos.remover(paciente.getId(), responsavel.getId());
        assertTrue(cenario.vinculos.listarPacientesDoResponsavel(responsavel.getId()).isEmpty());
        assertTrue(cenario.vinculos.listarResponsaveisDoPaciente(paciente.getId()).isEmpty());
    }

    @Test
    void cepNuloDeveSerRecusadoSemChamarAApi() {
        EnderecoService servico = new EnderecoService(cep -> {
            throw new AssertionError("nao deveria chamar a API");
        });
        assertThrows(DadoInvalidoException.class, () -> servico.buscarPorCep(null));
        assertThrows(DadoInvalidoException.class, () -> servico.buscarPorCep("1234-5678-9"));
    }
}
