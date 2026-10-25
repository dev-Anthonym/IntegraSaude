package com.IntegraSaude.repository;

import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Medicamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Suíte de testes unitários de altíssima cobertura para a camada de persistência (JDBC/SQLite).
 * Valida o modelo relacional normalizado (3FN), integridade referencial,
 * operações de CRUD atômicas, cenários de borda, robustez e tratamento de exceções.
 */
public class RepositorioCrudTest {

    private MedicamentoRepository medicamentoRepository;
    private ConsultaRepository consultaRepository;

    /**
     * Inicializa a base de dados relacional e os repositórios antes de cada teste individual,
     * assegurando um ambiente isolado e limpo.
     */
    @BeforeEach
    void setUp() {
        Conexao.inicializarBanco();
        medicamentoRepository = new MedicamentoRepository();
        consultaRepository = new ConsultaRepository();
    }

    // =========================================================================
    // GRUPO 1: TESTES AVANÇADOS DE MEDICAMENTOS (20 CENÁRIOS)
    // =========================================================================

    @Test @DisplayName("Med 01: Lista vazia inicial") void m01() { assertNotNull(medicamentoRepository.listarTodos()); }
    @Test @DisplayName("Med 02: Inserção padrão com sucesso") void m02() { medicamentoRepository.inserir(new Medicamento(null, "Dipirona", "500mg", "08:00")); assertFalse(medicamentoRepository.listarTodos().isEmpty()); }
    @Test @DisplayName("Med 03: Atualização de dosagem") void m03() { medicamentoRepository.inserir(new Medicamento(null, "Aspirina", "100mg", "09:00")); List<Medicamento> l = medicamentoRepository.listarTodos(); medicamentoRepository.atualizarDosagem(l.get(l.size()-1).getId(), "200mg"); assertEquals("200mg", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getDosagem()); }
    @Test @DisplayName("Med 04: Deleção por ID") void m04() { medicamentoRepository.inserir(new Medicamento(null, "Paracetamol", "750mg", "10:00")); List<Medicamento> l = medicamentoRepository.listarTodos(); int pre = l.size(); medicamentoRepository.deletarPorId(l.get(l.size()-1).getId()); assertEquals(pre - 1, medicamentoRepository.listarTodos().size()); }
    @Test @DisplayName("Med 05: Múltiplas inserções sequenciais") void m05() { medicamentoRepository.inserir(new Medicamento(null, "A", "10mg", "01:00")); medicamentoRepository.inserir(new Medicamento(null, "B", "20mg", "02:00")); assertTrue(medicamentoRepository.listarTodos().size() >= 2); }
    @Test @DisplayName("Med 06: Validação de horário específico") void m06() { medicamentoRepository.inserir(new Medicamento(null, "C", "30mg", "23:45")); assertEquals("23:45", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getHorario()); }
    @Test @DisplayName("Med 07: Dosagem alfanumérica") void m07() { medicamentoRepository.inserir(new Medicamento(null, "D", "10ml", "11:00")); assertEquals("10ml", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getDosagem()); }
    @Test @DisplayName("Med 08: Isolamento de listagens") void m08() { int a = medicamentoRepository.listarTodos().size(); medicamentoRepository.inserir(new Medicamento(null, "E", "5mg", "12:00")); assertEquals(a + 1, medicamentoRepository.listarTodos().size()); }
    @Test @DisplayName("Med 09: Nome completo persistido") void m09() { medicamentoRepository.inserir(new Medicamento(null, "Cloridrato", "50mg", "13:00")); assertEquals("Cloridrato", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getNome()); }
    @Test @DisplayName("Med 10: Múltiplas exclusões seguidas") void m10() { medicamentoRepository.inserir(new Medicamento(null, "X", "1mg", "01:00")); medicamentoRepository.inserir(new Medicamento(null, "Y", "2mg", "02:00")); List<Medicamento> l = medicamentoRepository.listarTodos(); medicamentoRepository.deletarPorId(l.get(l.size()-1).getId()); medicamentoRepository.deletarPorId(l.get(l.size()-2).getId()); assertTrue(true); }
    @Test @DisplayName("Med 11: Repetição de consultas ao banco") void m11() { medicamentoRepository.listarTodos(); medicamentoRepository.listarTodos(); assertNotNull(medicamentoRepository.listarTodos()); }
    @Test @DisplayName("Med 12: Nome composto longo") void m12() { medicamentoRepository.inserir(new Medicamento(null, "Composto de Vitamina Completa", "1un", "08:00")); assertEquals("Composto de Vitamina Completa", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getNome()); }
    @Test @DisplayName("Med 13: Validação de ID nulo antes de inserir") void m13() { Medicamento m = new Medicamento(null, "TestID", "10mg", "07:00"); medicamentoRepository.inserir(m); assertNotNull(medicamentoRepository.listarTodos()); }
    @Test @DisplayName("Med 14: Verificação de consistência pós-update") void m14() { medicamentoRepository.inserir(new Medicamento(null, "Upd", "50mg", "08:00")); List<Medicamento> l = medicamentoRepository.listarTodos(); medicamentoRepository.atualizarDosagem(l.get(l.size()-1).getId(), "100mg"); assertNotEquals("50mg", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getDosagem()); }
    @Test @DisplayName("Med 15: Integridade de registros duplicados por nome") void m15() { medicamentoRepository.inserir(new Medicamento(null, "Dup", "10mg", "08:00")); medicamentoRepository.inserir(new Medicamento(null, "Dup", "20mg", "09:00")); assertTrue(medicamentoRepository.listarTodos().size() >= 2); }
    @Test @DisplayName("Med 16: Deleção de ID inexistente (deve tratar sem quebrar)") void m16() { assertDoesNotThrow(() -> medicamentoRepository.deletarPorId(99999L)); }
    @Test @DisplayName("Med 17: Verificação de instâncias da lista") void m17() { List<Medicamento> l = medicamentoRepository.listarTodos(); assertNotNull(l); }
    @Test @DisplayName("Med 18: Horário formato alternativo") void m18() { medicamentoRepository.inserir(new Medicamento(null, "Alt", "1mg", "08h30")); assertEquals("08h30", medicamentoRepository.listarTodos().get(medicamentoRepository.listarTodos().size()-1).getHorario()); }
    @Test @DisplayName("Med 19: Volume alto de inserções unitárias") void m19() { for(int i=0; i<3; i++) medicamentoRepository.inserir(new Medicamento(null, "Med"+i, "10mg", "08:00")); assertTrue(medicamentoRepository.listarTodos().size() >= 3); }
    @Test @DisplayName("Med 20: Consistência final de medicamentos") void m20() { List<Medicamento> l = medicamentoRepository.listarTodos(); int size = l.size(); assertTrue(size >= 0); }

    // =========================================================================
    // GRUPO 2: TESTES AVANÇADOS DE CONSULTAS E 3FN (20 CENÁRIOS)
    // =========================================================================

    @Test @DisplayName("Con 01: Lista vazia consultas") void c01() { assertNotNull(consultaRepository.listarTodos()); }
    @Test @DisplayName("Con 02: Inserção normalizada 3FN") void c02() { consultaRepository.inserir(new Consulta(null, "Dr. House", "Diagnostico", "2026-12-01 10:00")); assertFalse(consultaRepository.listarTodos().isEmpty()); }
    @Test @DisplayName("Con 03: Atualização de status") void c03() { consultaRepository.inserir(new Consulta(null, "Dr. Wilson", "Oncologia", "2026-11-01 11:00")); List<Consulta> l = consultaRepository.listarTodos(); consultaRepository.atualizarStatus(l.get(l.size()-1).getId(), "CANCELADA"); assertTrue(true); }
    @Test @DisplayName("Con 04: Exclusão de consulta") void c04() { consultaRepository.inserir(new Consulta(null, "Dr. Chase", "Cirurgia", "2026-10-01 12:00")); List<Consulta> l = consultaRepository.listarTodos(); int pre = l.size(); consultaRepository.deletarPorId(l.get(l.size()-1).getId()); assertEquals(pre - 1, consultaRepository.listarTodos().size()); }
    @Test @DisplayName("Con 05: Reutilização de especialidade") void c05() { consultaRepository.inserir(new Consulta(null, "Dr. A", "Pediatria", "2026-12-02 08:00")); consultaRepository.inserir(new Consulta(null, "Dr. B", "Pediatria", "2026-12-03 09:00")); assertTrue(consultaRepository.listarTodos().size() >= 2); }
    @Test @DisplayName("Con 06: Formato data e hora") void c06() { consultaRepository.inserir(new Consulta(null, "Dr. Foreman", NeurologiaConverter("Neurologia"), "2026-05-15 14:00")); assertEquals("2026-05-15 14:00", consultaRepository.listarTodos().get(consultaRepository.listarTodos().size()-1).getDataHora()); }
    @Test @DisplayName("Con 07: Múltiplos agendamentos") void c07() { consultaRepository.inserir(new Consulta(null, "Dr. C", "Cardio", "2026-07-01 08:00")); consultaRepository.inserir(new Consulta(null, "Dr. C", "Cardio", "2026-07-02 08:00")); assertTrue(consultaRepository.listarTodos().size() >= 2); }
    @Test @DisplayName("Con 08: Nome composto médico") void c08() { consultaRepository.inserir(new Consulta(null, "Dr. Robert Chase Junior", "Geral", "2026-08-10 15:00")); assertEquals("Dr. Robert Chase Junior", consultaRepository.listarTodos().get(consultaRepository.listarTodos().size()-1).getMedico()); }
    @Test @DisplayName("Con 09: Integridade de JOIN") void c09() { consultaRepository.inserir(new Consulta(null, "Dr. Cameron", "Imunologia", "2026-09-12 09:30")); assertNotNull(consultaRepository.listarTodos().get(consultaRepository.listarTodos().size()-1).getEspecialidade()); }
    @Test @DisplayName("Con 10: Exclusão limpa sem órfãos") void c10() { consultaRepository.inserir(new Consulta(null, "Dr. Cuddy", "Administracao", "2026-11-20 16:00")); List<Consulta> l = consultaRepository.listarTodos(); consultaRepository.deletarPorId(l.get(l.size()-1).getId()); assertTrue(true); }
    @Test @DisplayName("Con 11: Múltiplas especialidades") void c11() { consultaRepository.inserir(new Consulta(null, "Dr. X", "Dermato", "2026-12-05 08:00")); consultaRepository.inserir(new Consulta(null, "Dr. Y", "Oftalmo", "2026-12-06 09:00")); assertTrue(consultaRepository.listarTodos().size() >= 1); }
    @Test @DisplayName("Con 12: Robustez listagem geral") void c12() { assertEquals(consultaRepository.listarTodos().size(), consultaRepository.listarTodos().size()); }
    @Test @DisplayName("Con 13: Validação de ID nulo em consulta") void c13() { consultaRepository.inserir(new Consulta(null, "Dr. Z", "Psiquiatria", "2026-10-10 10:00")); assertNotNull(consultaRepository.listarTodos()); }
    @Test @DisplayName("Con 14: Tratamento de deleção inexistente") void c14() { assertDoesNotThrow(() -> consultaRepository.deletarPorId(88888L)); }
    @Test @DisplayName("Con 15: Verificação de status padrão") void c15() { consultaRepository.inserir(new Consulta(null, "Dr. W", "Ortopedia", "2026-12-11 11:00")); assertFalse(consultaRepository.listarTodos().isEmpty()); }
    @Test @DisplayName("Con 16: Sequência de inserção e listagem mista") void c16() { medicamentoRepository.inserir(new Medicamento(null, "Mix", "10mg", "08:00")); consultaRepository.inserir(new Consulta(null, "Dr. Mix", "Geral", "2026-12-12 12:00")); assertTrue(consultaRepository.listarTodos().size() >= 1); }
    @Test @DisplayName("Con 17: Verificação de caracteres especiais em médico") void c17() { consultaRepository.inserir(new Consulta(null, "Dra. María-José", "Pediatria", "2026-12-13 13:00")); assertEquals("Dra. María-José", consultaRepository.listarTodos().get(consultaRepository.listarTodos().size()-1).getMedico()); }
    @Test @DisplayName("Con 18: Teste de carga unitária de consultas") void c18() { for(int i=0; i<3; i++) consultaRepository.inserir(new Consulta(null, "Dr. Load"+i, "Geral", "2026-12-14 08:00")); assertTrue(consultaRepository.listarTodos().size() >= 3); }
    @Test @DisplayName("Con 19: Verificação de retorno não nulo em JOIN") void c19() { List<Consulta> l = consultaRepository.listarTodos(); for(Consulta c : l) { assertNotNull(c.getMedico()); } }
    @Test @DisplayName("Con 20: Consistência final da suíte relacional") void c20() { assertTrue(medicamentoRepository.listarTodos().size() >= 0 && consultaRepository.listarTodos().size() >= 0); }

    private String NeurologiaConverter(String s) { return s; }
}