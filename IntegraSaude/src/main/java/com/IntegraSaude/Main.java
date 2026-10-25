package com.IntegraSaude;

import com.IntegraSaude.exception.ValidacaoException;
import com.IntegraSaude.service.ConsultaService;
import com.IntegraSaude.service.MedicamentoService;

/**
 * A classe Main é o ponto de entrada da aplicação IntegraSaude.
 * Focada temporariamente na validação das regras de negócio.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Iniciando o Sistema IntegraSaude (Modo Teste de Regras) ===");

        // Instancia os serviços
        MedicamentoService medicamentoService = new MedicamentoService();
        ConsultaService consultaService = new ConsultaService();

        try {
            // Testando a validação de Medicamento
            System.out.println("\nTestando validação de medicamento...");
            medicamentoService.cadastrarMedicamento("", "500mg", "08:00");

        } catch (ValidacaoException e) {
            System.err.println("-> Sucesso! Erro capturado no medicamento: " + e.getMessage());
        }

        try {
            // Testando a validação de Consulta
            System.out.println("\nTestando validação de consulta...");
            consultaService.cadastrarConsulta("", "Cardiologia", "2026-11-15 10:00");

        } catch (ValidacaoException e) {
            System.err.println("-> Sucesso! Erro capturado na consulta: " + e.getMessage());
        }

        System.out.println("\n=== Aplicação executada com sucesso! ===");
    }
}