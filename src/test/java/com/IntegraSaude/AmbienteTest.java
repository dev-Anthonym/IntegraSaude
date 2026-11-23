package com.IntegraSaude;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.config.ConfiguracaoApp;
import com.IntegraSaude.exception.DadoInvalidoException;
import org.junit.jupiter.api.Test;

/** Prova que o ambiente de testes funciona (Surefire + JUnit 5) e que a configuração padrão está certa. */
class AmbienteTest {

    @Test
    void deveExecutarTestesJUnit() {
        assertTrue(true);
    }

    @Test
    void deveCarregarConfiguracaoPadrao() {
        ConfiguracaoApp configuracao = ConfiguracaoApp.carregar();
        assertEquals(30, configuracao.lembretes().tempoLimiteMinutos());
    }

    @Test
    void deveReduzirTempoLimiteNoModoDemonstracao() {
        ConfiguracaoApp configuracao = ConfiguracaoApp.carregar("--demo");
        assertEquals(1, configuracao.lembretes().tempoLimiteMinutos());
        assertTrue(configuracao.carregarDemonstracao());
    }

    @Test
    void deveAtivarModoWebComPortaInformada() {
        ConfiguracaoApp padrao = ConfiguracaoApp.carregar();
        assertFalse(padrao.modoWeb());
        assertEquals(ConfiguracaoApp.PORTA_WEB_PADRAO, padrao.portaWeb());

        ConfiguracaoApp web = ConfiguracaoApp.carregar("--web", "--porta=9090");
        assertTrue(web.modoWeb());
        assertEquals(9090, web.portaWeb());
    }

    @Test
    void deveRejeitarPortaForaDoIntervalo() {
        assertThrows(DadoInvalidoException.class, () -> ConfiguracaoApp.carregar("--porta=70000"));
    }
}
