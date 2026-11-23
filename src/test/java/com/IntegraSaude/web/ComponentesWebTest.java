package com.IntegraSaude.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.RelogioAjustavel;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import com.IntegraSaude.service.Servicos;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Testes de unidade das peças da camada web: sessões, parâmetros, JSON e servidor. */
class ComponentesWebTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0);

    // ---------------------------------------------------------------- SessoesWeb

    @Test
    void sessaoDeveValerAteOLimiteEExpirarDepois() {
        RelogioAjustavel relogio = new RelogioAjustavel(AGORA);
        SessoesWeb sessoes = new SessoesWeb(relogio);
        String token = sessoes.abrir(7L);

        relogio.avancar(SessoesWeb.DURACAO);
        assertEquals(Optional.of(7L), sessoes.usuarioDo(token));
        relogio.avancar(Duration.ofNanos(1));
        assertEquals(Optional.empty(), sessoes.usuarioDo(token));
        // Depois de expirar, a sessão é apagada: voltar o relógio não a ressuscita
        relogio.avancar(Duration.ofHours(-1));
        assertEquals(Optional.empty(), sessoes.usuarioDo(token));
    }

    @Test
    void tokensDevemSerUnicosEEncerraveis() {
        SessoesWeb sessoes = new SessoesWeb(new RelogioAjustavel(AGORA));
        String primeiro = sessoes.abrir(1L);
        String segundo = sessoes.abrir(1L);
        assertNotEquals(primeiro, segundo);
        assertTrue(primeiro.length() >= 43, "32 bytes em Base64 sem padding");

        sessoes.encerrar(primeiro);
        sessoes.encerrar(null);
        sessoes.encerrar("inexistente");
        assertEquals(Optional.empty(), sessoes.usuarioDo(primeiro));
        assertEquals(Optional.of(1L), sessoes.usuarioDo(segundo));
        assertEquals(Optional.empty(), sessoes.usuarioDo(null));
    }

    // ---------------------------------------------------------------- Parametros

    @Test
    void parametrosDevemJuntarQueryECorpoValendoOUltimo() {
        Parametros p = Parametros.de("a=1&b=query", null, "", "b=corpo&&c&=sem-chave");
        assertEquals("1", p.texto("a"));
        assertEquals("corpo", p.texto("b"));
        assertEquals("", p.texto("c"));
        assertEquals("sem-chave", p.texto(""));
        assertEquals("", p.texto("ausente"));
        assertEquals(Optional.of("x"), Parametros.de("t=++x++").opcional("t"));
    }

    @Test
    void conversoesInvalidasDevemVirarDadoInvalido() {
        Parametros p = Parametros.de("n=12a&id=&d=2026-02-30&h=24:00&grande=99999999999");
        assertTrue(assertThrows(DadoInvalidoException.class, () -> p.inteiro("n", "Frequencia")).getMessage()
                .contains("Frequencia"));
        assertThrows(DadoInvalidoException.class, () -> p.inteiro("grande", "X"));
        assertThrows(DadoInvalidoException.class, () -> p.id("id"));
        assertThrows(DadoInvalidoException.class, () -> p.data("d", "Data"));
        assertThrows(DadoInvalidoException.class, () -> p.hora("h", "Hora"));
        assertThrows(DadoInvalidoException.class, () -> p.dataOpcional("d"));
        assertThrows(DadoInvalidoException.class, () -> Parametros.de("x=%"));
        assertEquals(99_999_999_999L, p.id("grande"));
    }

    @Test
    void conversoesValidas() {
        Parametros p = Parametros.de("n=-3&d=2026-11-23&h=08:05");
        assertEquals(-3, p.inteiro("n", "N"));
        assertEquals(LocalDate.of(2026, 11, 23), p.data("d", "D"));
        assertEquals(LocalTime.of(8, 5), p.hora("h", "H"));
        assertEquals(Optional.of(LocalDate.of(2026, 11, 23)), p.dataOpcional("d"));
        assertEquals(Optional.empty(), p.dataOpcional("ausente"));
    }

    // ---------------------------------------------------------------- Json

    @Test
    void jsonDeveEscaparControlesESeparadoresDeLinha() {
        assertEquals("\"\\t\\r\\u0001\\u0026\\u2028\\u2029\"", Json.escrever("\t\r\u0001&  "));
        assertEquals("[]", Json.escrever(List.of()));
        assertEquals("{}", Json.escrever(Json.objeto()));
        assertEquals("[\"PACIENTE\",2.5,{\"k\":false}]",
                Json.escrever(List.of(Perfil.PACIENTE, 2.5, Json.objeto("k", false))));
        assertEquals("\"08:00\"", Json.escrever(LocalTime.of(8, 0)));
    }

    @Test
    void jsonDeveRecusarParesIncompletosETiposDesconhecidos() {
        assertThrows(IllegalArgumentException.class, () -> Json.objeto("so-chave"));
        assertThrows(IllegalArgumentException.class, () -> Json.escrever(new Object()));
    }

    // ---------------------------------------------------------------- ServidorWeb

    @Test
    void servidorDeveInformarEnderecoEFalharComPortaOcupada() throws Exception {
        Servicos servicos = Servicos.montar(Repositorios.emMemoria(), ConfiguracaoLembretes.padrao(),
                new RelogioAjustavel(AGORA), (u, m) -> { }, cep -> null);
        try (ServerSocket ocupada = new ServerSocket(0, 0, java.net.InetAddress.getLoopbackAddress())) {
            OperacaoInvalidaException erro = assertThrows(OperacaoInvalidaException.class,
                    () -> new ServidorWeb(servicos, ocupada.getLocalPort()));
            assertTrue(erro.getMessage().contains(String.valueOf(ocupada.getLocalPort())));
        }
        ServidorWeb servidor = new ServidorWeb(servicos, 0);
        servidor.iniciar();
        try {
            assertEquals("http://localhost:" + servidor.porta() + "/", servidor.endereco());
        } finally {
            servidor.parar();
        }
    }

    @Test
    void primeiroNomeDeveIgnorarEspacosNasPontas() {
        assertEquals("Maria", VisaoLembretes.primeiroNome("  Maria Souza "));
        assertEquals("Ana", VisaoLembretes.primeiroNome("Ana"));
    }
}
