package com.IntegraSaude.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.exception.DadoInvalidoException;
import org.junit.jupiter.api.Test;

/** Leitura dos argumentos de linha de comando e seus limites. */
class ConfiguracaoAppTest {

    @Test
    void deveLerOsValoresDoConfigProperties() {
        ConfiguracaoApp padrao = ConfiguracaoApp.carregar();
        assertEquals(60, padrao.lembretes().antecedenciaConsultaMinutos());
        assertEquals(30, padrao.intervaloAgendadorSegundos());
        assertEquals("database/integrasaude.db", padrao.caminhoBanco());
        assertFalse(padrao.usarMemoria());
        assertFalse(padrao.carregarDemonstracao());
    }

    @Test
    void argumentosDevemTerPrioridadeSobreODemo() {
        ConfiguracaoApp config = ConfiguracaoApp.carregar("--demo", "--tempo-limite=5", "--intervalo=2",
                "--memoria", "--banco=outro/arquivo.db");
        assertEquals(5, config.lembretes().tempoLimiteMinutos());
        assertEquals(2, config.intervaloAgendadorSegundos());
        assertTrue(config.usarMemoria());
        assertEquals("outro/arquivo.db", config.caminhoBanco());
    }

    @Test
    void demoSemOutrosArgumentosDeveRodarACada10Segundos() {
        assertEquals(10, ConfiguracaoApp.carregar("--demo").intervaloAgendadorSegundos());
    }

    @Test
    void portaDeveAceitarOsLimitesDoIntervalo() {
        assertEquals(0, ConfiguracaoApp.carregar("--porta=0").portaWeb());
        assertEquals(65_535, ConfiguracaoApp.carregar("--porta=65535").portaWeb());
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("--porta=-1"));
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("--porta=65536"));
    }

    @Test
    void deveRejeitarArgumentosInvalidos() {
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("demo"));
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("--tempo-limite=abc"));
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("--tempo-limite=0"));
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("--intervalo="));
        DadoInvalidoException erro = assertThrows(DadoInvalidoException.class,
                () -> ConfiguracaoApp.carregar("--porta=oito"));
        assertTrue(erro.getMessage().contains("porta"));
    }

    @Test
    void valorComEspacosDeveSerAceito() {
        assertEquals(45, ConfiguracaoApp.carregar("--tempo-limite= 45 ").lembretes().tempoLimiteMinutos());
    }
}
