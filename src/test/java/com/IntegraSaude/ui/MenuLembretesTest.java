package com.IntegraSaude.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.MedicamentoAgendado;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.HistoricoRepository;
import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import com.IntegraSaude.service.AutenticacaoService;
import com.IntegraSaude.service.GeradorLembretes;
import com.IntegraSaude.service.HistoricoService;
import com.IntegraSaude.service.LembreteService;
import com.IntegraSaude.service.UsuarioService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MenuLembretesTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DIA = LocalDate.of(2026, 10, 24);
    private static final Long PACIENTE_ID = 10L;

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private LembreteRepository lembreteRepository;
    private HistoricoRepository historicoRepository;

    @BeforeEach
    void setUp() {
        AutenticacaoService.logout();
        lembreteRepository = new LembreteRepositoryMemoria();
        historicoRepository = new HistoricoRepositoryMemoria();
    }

    @AfterEach
    void tearDown() {
        AutenticacaoService.logout();
    }

    @Test
    void deveGerarEListarLembretesPendentesDoDia() {
        menuEm(LocalTime.of(7, 0), "10\n").verPendentes();

        String texto = textoExibido();
        assertTrue(texto.contains("Losartana 50mg"), texto);
        assertTrue(texto.contains("08:00"), texto);
        assertTrue(texto.contains("20:00"), texto);
    }

    @Test
    void deveUsarPacienteLogadoSemPerguntarOId() {
        UsuarioRepositoryMemoria usuarios = new UsuarioRepositoryMemoria();
        UsuarioService usuarioService = new UsuarioService(usuarios);
        Usuario ana = usuarioService.cadastrar("Ana Maria", "ana@email.com", "Senha123", Perfil.PACIENTE);
        new AutenticacaoService(usuarios, usuarioService).login("ana@email.com", "Senha123");

        menuEm(LocalTime.of(7, 0), "").verPendentes();

        assertFalse(textoExibido().contains("ID do paciente"));
        assertEquals(2, lembreteRepository.listarPendentesDoPaciente(ana.getId()).size());
    }

    @Test
    void deveConfirmarLembreteDoPaciente() {
        Lembrete lembrete = gerarLembretes().get(0);

        menuEm(LocalTime.of(8, 10), "10\n" + lembrete.getId() + "\n").confirmarLembrete();

        assertTrue(textoExibido().contains("tomei"), textoExibido());
        assertEquals(StatusLembrete.CONFIRMADO, lembrete.getStatus());
    }

    @Test
    void deveMostrarMensagemClaraParaLembreteInexistente() {
        gerarLembretes();

        menuEm(LocalTime.of(8, 10), "10\n999\n").confirmarLembrete();

        assertTrue(textoExibido().contains("não encontrado"), textoExibido());
        assertSemStackTrace();
    }

    @Test
    void deveMostrarMensagemClaraQuandoPrazoExpirou() {
        Lembrete lembrete = gerarLembretes().get(0);

        menuEm(LocalTime.of(9, 0), "10\n" + lembrete.getId() + "\n").confirmarLembrete();

        assertTrue(textoExibido().contains("prazo"), textoExibido());
        assertSemStackTrace();
    }

    @Test
    void naoDeveDerrubarComEntradaInvalida() {
        MenuLembretes menu = menuEm(LocalTime.of(7, 0), "abc\n");

        assertDoesNotThrow(menu::confirmarLembrete);
        assertTrue(textoExibido().contains("número válido"), textoExibido());
        assertSemStackTrace();
    }

    @Test
    void naoDeveDerrubarQuandoEntradaTermina() {
        MenuLembretes menu = menuEm(LocalTime.of(7, 0), "");

        assertDoesNotThrow(menu::verHistorico);
        assertSemStackTrace();
    }

    @Test
    void deveMostrarHistoricoDoPaciente() {
        Lembrete lembrete = gerarLembretes().get(0);
        menuEm(LocalTime.of(8, 5), "10\n" + lembrete.getId() + "\n").confirmarLembrete();
        saida.reset();

        menuEm(LocalTime.of(8, 6), "10\n").verHistorico();

        String texto = textoExibido();
        assertTrue(texto.contains("GERADO"), texto);
        assertTrue(texto.contains("CONFIRMADO"), texto);
        assertTrue(texto.contains("Losartana 50mg"), texto);
    }

    private List<Lembrete> gerarLembretes() {
        menuEm(LocalTime.of(7, 0), "10\n").verPendentes();
        saida.reset();
        return lembreteRepository.listarPendentesDoPaciente(PACIENTE_ID);
    }

    private MenuLembretes menuEm(LocalTime horario, String entradaDigitada) {
        Clock relogio = Clock.fixed(LocalDateTime.of(DIA, horario).atZone(FUSO).toInstant(), FUSO);
        HistoricoService historico = new HistoricoService(historicoRepository, lembreteRepository, relogio);
        LembreteService lembreteService = new LembreteService(lembreteRepository, historico, relogio,
                Duration.ofMinutes(30));
        GeradorLembretes gerador = new GeradorLembretes(
                pacienteId -> List.of(new MedicamentoAgendado(5L, "Losartana 50mg", DIA, null)),
                (medicamentoId, dia) -> List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)),
                pacienteId -> List.of(),
                lembreteRepository, historico, Duration.ofMinutes(60));
        return new MenuLembretes(new Scanner(entradaDigitada), new PrintStream(saida, true, StandardCharsets.UTF_8),
                lembreteService, historico, gerador, relogio);
    }

    private String textoExibido() {
        return saida.toString(StandardCharsets.UTF_8);
    }

    private void assertSemStackTrace() {
        assertFalse(textoExibido().contains("Exception"), textoExibido());
        assertFalse(textoExibido().contains("\tat "), textoExibido());
    }
}
