package com.IntegraSaude.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Igualdade por id das entidades e transições de estado da consulta e do alerta. */
class EntidadeBaseTest {

    private static Vinculo vinculo(Long id) {
        Vinculo v = new Vinculo(1L, 2L);
        v.setId(id);
        return v;
    }

    @Test
    void entidadesComMesmoIdEMesmaClasseSaoIguais() {
        assertEquals(vinculo(5L), vinculo(5L));
        assertEquals(vinculo(5L).hashCode(), vinculo(5L).hashCode());
        assertNotEquals(vinculo(5L), vinculo(6L));
    }

    @Test
    void entidadeSemIdSoEIgualAElaMesma() {
        Vinculo semId = vinculo(null);
        assertEquals(semId, semId);
        assertNotEquals(semId, vinculo(null));
        assertNotEquals(vinculo(null), vinculo(5L));
        Set<Vinculo> conjunto = new HashSet<>(Set.of(semId));
        assertTrue(conjunto.contains(semId));
    }

    @Test
    void classesDiferentesOuNuloNaoSaoIguais() {
        Especialidade especialidade = new Especialidade("Cardiologia");
        especialidade.setId(5L);
        assertNotEquals(vinculo(5L), especialidade);
        assertNotEquals(null, vinculo(5L));
    }

    @Test
    void consultaCanceladaNaoVoltaAtras() {
        Consulta consulta = new Consulta(1L, null, "Local", LocalDateTime.of(2026, 11, 24, 9, 0));
        consulta.cancelar();
        assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
        assertThrows(OperacaoInvalidaException.class, consulta::cancelar);
        assertThrows(OperacaoInvalidaException.class, () ->
                new Consulta(1L, null, "L", LocalDateTime.of(2026, 11, 24, 9, 0), StatusConsulta.REALIZADA).cancelar());
    }

    @Test
    void alertaSoPodeSerMarcadoEmLembretePerdido() {
        Lembrete lembrete = new Lembrete(1L, TipoLembrete.MEDICAMENTO, 1L, LocalDateTime.of(2026, 11, 23, 10, 0), "x");
        assertThrows(OperacaoInvalidaException.class, lembrete::marcarAlertaEnviado);
        lembrete.marcarComoPerdido();
        lembrete.marcarAlertaEnviado();
        assertTrue(lembrete.isAlertaEnviado());
        assertThrows(OperacaoInvalidaException.class, lembrete::marcarComoPerdido);
    }
}
