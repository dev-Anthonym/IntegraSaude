package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.repository.jdbc.ConexaoBanco;
import com.IntegraSaude.repository.jdbc.InicializadorBanco;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Classe de testes unitários e de integração com SQLite para validar o CRUD completo
 * dos repositórios JDBC e as restrições de integridade (Constraints e 3FN) do banco de dados.
 */
public class RepositorioCrudTest {

    private File arquivoBancoTemp;
    private UsuarioRepository usuarioRepository;
    private MedicamentoRepository medicamentoRepository;
    private ConsultaRepository consultaRepository;

    /**
     * Antes de cada teste, cria um arquivo SQLite temporário limpo e executa os scripts DDL.
     */
    @BeforeEach
    void configurarBancoTemporario() throws Exception {
        arquivoBancoTemp = File.createTempFile("integrasaude_teste_", ".db");
        ConexaoBanco.definirUrlTeste(arquivoBancoTemp.getAbsolutePath());

        try (Connection conn = ConexaoBanco.abrir()) {
            InicializadorBanco.executar(conn);
        }

        usuarioRepository = new UsuarioRepository();
        medicamentoRepository = new MedicamentoRepository();
        consultaRepository = new ConsultaRepository();
    }

    /**
     * Após cada teste, restaura a URL padrão e remove o arquivo temporário.
     */
    @AfterEach
    void limparBancoTemporario() {
        ConexaoBanco.restaurarUrlPadrao();
        if (arquivoBancoTemp != null && arquivoBancoTemp.exists()) {
            arquivoBancoTemp.delete();
        }
    }

    @Test
    void deveExecutarCrudCompletoDeUsuario() {
        // 1. CREATE (Inserir)
        usuarioRepository.inserir("Maria Souza", "maria@teste.com", "HASH123", "PACIENTE", "2026-10-25T10:00:00");

        // 2. READ (Buscar por ID e Listar)
        Optional<String> nomeBuscado = usuarioRepository.buscarNomePorId(1);
        assertTrue(nomeBuscado.isPresent());
        assertEquals("Maria Souza", nomeBuscado.get());

        List<String> emails = usuarioRepository.listarEmails();
        assertEquals(1, emails.size());
        assertEquals("maria@teste.com", emails.get(0));

        // 3. UPDATE (Atualizar nome)
        usuarioRepository.atualizarNome(1, "Maria Souza Silva");
        assertEquals("Maria Souza Silva", usuarioRepository.buscarNomePorId(1).orElse(""));

        // 4. DELETE (Remover)
        usuarioRepository.deletarPorId(1);
        assertFalse(usuarioRepository.buscarNomePorId(1).isPresent());
    }

    @Test
    void deveRejeitarUsuarioComEmailDuplicadoOuPerfilInvalido() {
        usuarioRepository.inserir("Joao", "joao@teste.com", "HASH", "PACIENTE", "2026-10-25T10:00:00");

        // Tenta inserir e-mail duplicado (viola UNIQUE)
        assertThrows(RuntimeException.class, () ->
            usuarioRepository.inserir("Outro Joao", "joao@teste.com", "HASH", "PACIENTE", "2026-10-25T11:00:00")
        );

        // Tenta inserir perfil inválido (viola CHECK constraint)
        assertThrows(RuntimeException.class, () ->
            usuarioRepository.inserir("Pedro", "pedro@teste.com", "HASH", "ADMIN_INVALIDO", "2026-10-25T11:00:00")
        );
    }

    @Test
    void deveExecutarCrudCompletoDeMedicamento() {
        // 1. CREATE
        Medicamento novoMed = new Medicamento(null, "Amoxicilina", "500mg", "08:00");
        medicamentoRepository.inserir(novoMed);

        // 2. READ
        List<Medicamento> lista = medicamentoRepository.listarTodos();
        assertEquals(1, lista.size());
        assertEquals("Amoxicilina", lista.get(0).getNome());
        assertEquals("500mg", lista.get(0).getDosagem());

        // 3. UPDATE
        long idGerado = lista.get(0).getId();
        medicamentoRepository.atualizarDosagem(idGerado, "875mg");
        assertEquals("875mg", medicamentoRepository.listarTodos().get(0).getDosagem());

        // 4. DELETE
        medicamentoRepository.deletarPorId(idGerado);
        assertTrue(medicamentoRepository.listarTodos().isEmpty());
    }

    @Test
    void deveExecutarCrudCompletoDeConsultaComRelacionamentos3FN() {
        // 1. CREATE (Cadastra especialidade, médico e consulta automaticamente na 3FN)
        Consulta novaConsulta = new Consulta(null, "Dr. Roberto", "Cardiologia", "2026-11-15T14:00:00");
        consultaRepository.inserir(novaConsulta);

        // 2. READ (Valida o JOIN entre consulta, medico e especialidade)
        List<Consulta> consultas = consultaRepository.listarTodos();
        assertEquals(1, consultas.size());
        assertEquals("Dr. Roberto", consultas.get(0).getMedico());
        assertEquals("Cardiologia", consultas.get(0).getEspecialidade());

        // 3. UPDATE
        long idConsulta = consultas.get(0).getId();
        consultaRepository.atualizarStatus(idConsulta, "REALIZADA");

        // 4. DELETE
        consultaRepository.deletarPorId(idConsulta);
        assertTrue(consultaRepository.listarTodos().isEmpty());
    }
}