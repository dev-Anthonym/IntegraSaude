package com.IntegraSaude.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.exception.PersistenciaException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoLembrete;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LembreteRepositoryJdbcTest {

    private static final Long PACIENTE_ID = 1L;
    private static final Long OUTRO_PACIENTE_ID = 2L;
    private static final Long MEDICAMENTO_ID = 1L;
    private static final Long CONSULTA_ID = 1L;
    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 24, 8, 0);

    private LembreteRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        repository = new LembreteRepositoryJdbc(BancoTeste.criar());
    }

    @Test
    void deveSalvarEBuscarLembreteDeMedicamento() {
        Lembrete salvo = repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO));

        Lembrete encontrado = repository.buscarPorId(salvo.getId()).orElseThrow();

        assertNotNull(salvo.getId());
        assertEquals(PACIENTE_ID, encontrado.getPacienteId());
        assertEquals(TipoLembrete.MEDICAMENTO, encontrado.getTipo());
        assertEquals(MEDICAMENTO_ID, encontrado.getItemId());
        assertEquals("Losartana 50mg", encontrado.getDescricao());
        assertEquals(HORARIO, encontrado.getHorarioPrevisto());
        assertEquals(StatusLembrete.PENDENTE, encontrado.getStatus());
    }

    @Test
    void deveSalvarLembreteDeConsulta() {
        Lembrete salvo = repository.salvar(new Lembrete(null, PACIENTE_ID, TipoLembrete.CONSULTA,
                CONSULTA_ID, "Consulta com cardiologista", HORARIO.plusHours(5)));

        Lembrete encontrado = repository.buscarPorId(salvo.getId()).orElseThrow();

        assertEquals(TipoLembrete.CONSULTA, encontrado.getTipo());
        assertEquals(CONSULTA_ID, encontrado.getItemId());
    }

    @Test
    void deveRetornarVazioParaLembreteInexistente() {
        assertTrue(repository.buscarPorId(404L).isEmpty());
    }

    @Test
    void deveAtualizarStatusEAlertaEnviado() {
        Lembrete lembrete = repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO));
        lembrete.marcarComoPerdido();
        lembrete.marcarAlertaEnviado();

        repository.atualizar(lembrete);

        Lembrete encontrado = repository.buscarPorId(lembrete.getId()).orElseThrow();
        assertEquals(StatusLembrete.PERDIDO, encontrado.getStatus());
        assertTrue(encontrado.isAlertaEnviado());
    }

    @Test
    void deveListarPendentesDoPacienteOrdenadosPorHorario() {
        Lembrete tarde = repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO.plusHours(12)));
        Lembrete manha = repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO));
        Lembrete confirmado = repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO.plusHours(4)));
        confirmado.confirmar();
        repository.atualizar(confirmado);
        repository.salvar(lembreteDeMedicamento(OUTRO_PACIENTE_ID, HORARIO.plusHours(1)));

        List<Lembrete> pendentes = repository.listarPendentesDoPaciente(PACIENTE_ID);

        assertEquals(List.of(manha.getId(), tarde.getId()), pendentes.stream().map(Lembrete::getId).toList());
        assertEquals(3, repository.listarPorPaciente(PACIENTE_ID).size());
        assertEquals(3, repository.listarPendentes().size());
    }

    @Test
    void deveRejeitarLembreteDuplicadoNoBanco() {
        repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO));

        assertThrows(PersistenciaException.class,
                () -> repository.salvar(lembreteDeMedicamento(PACIENTE_ID, HORARIO)));
    }

    private Lembrete lembreteDeMedicamento(Long pacienteId, LocalDateTime horario) {
        return new Lembrete(null, pacienteId, TipoLembrete.MEDICAMENTO, MEDICAMENTO_ID, "Losartana 50mg", horario);
    }
}
