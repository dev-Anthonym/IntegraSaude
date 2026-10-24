package com.IntegraSaude;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Classe de teste simples para validar que o JUnit e o ambiente de compilação estão funcionais
public class AmbienteTeste {

    @Test
    void deveValidarQueOAmbienteEstaFuncionando() {
        boolean ambienteOk = true;
        // Valida se a condição é verdadeira; se for, o teste passa verde
        assertTrue(ambienteOk, "O ambiente de testes do Maven com JUnit 5 deve estar operacional");
    }
}