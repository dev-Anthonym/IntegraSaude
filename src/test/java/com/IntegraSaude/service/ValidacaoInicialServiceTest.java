package com.IntegraSaude.service;

import com.IntegraSaude.exception.ValidacaoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacaoInicialServiceTest {

    private MedicamentoService medicamentoService;
    private ConsultaService consultaService;

    @BeforeEach
    void setUp() {
        medicamentoService = new MedicamentoService();
        consultaService = new ConsultaService();
    }

    @Test
    void deveRejeitarMedicamentoComNomeVazioOuNulo() {
        ValidacaoException exVazio = assertThrows(ValidacaoException.class, () ->
                medicamentoService.cadastrarMedicamento("   ", "500mg", "08:00"));
        assertEquals("O nome do medicamento não pode estar vazio.", exVazio.getMessage());

        assertThrows(ValidacaoException.class, () ->
                medicamentoService.cadastrarMedicamento(null, "500mg", "08:00"));
    }

    @Test
    void deveRejeitarMedicamentoComDosagemVazia() {
        assertThrows(ValidacaoException.class, () ->
                medicamentoService.cadastrarMedicamento("Dipirona", "", "08:00"));
    }

    @Test
    void deveRejeitarMedicamentoComHorarioVazio() {
        assertThrows(ValidacaoException.class, () ->
                medicamentoService.cadastrarMedicamento("Dipirona", "500mg", " "));
    }

    @Test
    void deveRejeitarConsultaComMedicoVazioOuNulo() {
        assertThrows(ValidacaoException.class, () ->
                consultaService.cadastrarConsulta("", "Cardiologia", "2026-11-15 10:00"));

        assertThrows(ValidacaoException.class, () ->
                consultaService.cadastrarConsulta(null, "Cardiologia", "2026-11-15 10:00"));
    }

    @Test
    void deveRejeitarConsultaComEspecialidadeVazia() {
        assertThrows(ValidacaoException.class, () ->
                consultaService.cadastrarConsulta("Dr. Carlos", "  ", "2026-11-15 10:00"));
    }

    @Test
    void deveRejeitarConsultaComDataHoraVazia() {
        assertThrows(ValidacaoException.class, () ->
                consultaService.cadastrarConsulta("Dr. Carlos", "Cardiologia", ""));
    }
}
