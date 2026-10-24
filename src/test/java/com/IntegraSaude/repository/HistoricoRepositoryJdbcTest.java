package com.IntegraSaude.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoricoRepositoryJdbcTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 24, 8, 0);

    private FabricaConexao fabrica;
    private HistoricoRepository historicoRepository;
    private Lembrete lembrete;

    @BeforeEach
    void setUp() throws SQLException {
        fabrica = BancoTeste.criar();
        historicoRepository = new HistoricoRepositoryJdbc(fabrica);
        lembrete = new LembreteRepositoryJdbc(fabrica).salvar(
                new Lembrete(null, 1L, TipoLembrete.MEDICAMENTO, 1L, "Losartana 50mg", HORARIO));
    }

    @Test
    void deveSalvarEListarEventosDoLembreteOrdenadosPorData() {
        historicoRepository.salvar(new EventoHistorico(null, lembrete.getId(), TipoEvento.CONFIRMADO, HORARIO.plusMinutes(5)));
        EventoHistorico gerado = historicoRepository.salvar(
                new EventoHistorico(null, lembrete.getId(), TipoEvento.GERADO, HORARIO.minusHours(8)));

        List<EventoHistorico> eventos = historicoRepository.listarPorLembrete(lembrete.getId());

        assertNotNull(gerado.getId());
        assertEquals(List.of(TipoEvento.GERADO, TipoEvento.CONFIRMADO),
                eventos.stream().map(EventoHistorico::getTipoEvento).toList());
        assertEquals(HORARIO.minusHours(8), eventos.get(0).getDataHora());
    }

    @Test
    void deveRetornarListaVaziaParaLembreteSemEventos() {
        assertTrue(historicoRepository.listarPorLembrete(lembrete.getId()).isEmpty());
    }

    @Test
    void deveCarregarDadosIniciaisDoModulo() throws SQLException {
        BancoTeste.executarScript(fabrica, "database/04_lembretes_dados.sql");

        LembreteRepository lembretes = new LembreteRepositoryJdbc(fabrica);

        assertEquals(4, lembretes.listarPorPaciente(1L).size());
        assertTrue(lembretes.listarPorPaciente(1L).stream()
                .anyMatch(l -> !historicoRepository.listarPorLembrete(l.getId()).isEmpty()));
    }
}
