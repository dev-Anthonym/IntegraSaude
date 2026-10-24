package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.exception.TransicaoInvalidaException;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LembreteServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final Duration TEMPO_LIMITE = Duration.ofMinutes(30);
    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 24, 8, 0);
    private static final Long PACIENTE_ID = 10L;

    private LembreteRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LembreteRepositoryMemoria();
    }

    @Test
    void deveConfirmarLembreteDentroDoPrazo() {
        Lembrete lembrete = salvarLembrete(PACIENTE_ID, HORARIO);

        servicoEm(HORARIO.plusMinutes(10)).confirmar(lembrete.getId());

        assertEquals(StatusLembrete.CONFIRMADO, buscar(lembrete).getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoAposOTempoLimite() {
        Lembrete lembrete = salvarLembrete(PACIENTE_ID, HORARIO);
        LembreteService service = servicoEm(HORARIO.plusMinutes(31));

        assertThrows(TransicaoInvalidaException.class, () -> service.confirmar(lembrete.getId()));
        assertEquals(StatusLembrete.PENDENTE, buscar(lembrete).getStatus());
    }

    @Test
    void deveRejeitarConfirmacaoDeLembretePerdido() {
        Lembrete lembrete = salvarLembrete(PACIENTE_ID, HORARIO);
        LembreteService service = servicoEm(HORARIO);
        service.verificarPerdidos(HORARIO.plusMinutes(31));

        assertThrows(TransicaoInvalidaException.class, () -> service.confirmar(lembrete.getId()));
    }

    @Test
    void deveMarcarComoPerdidoAposOTempoLimite() {
        Lembrete lembrete = salvarLembrete(PACIENTE_ID, HORARIO);

        List<Lembrete> perdidos = servicoEm(HORARIO).verificarPerdidos(HORARIO.plusMinutes(31));

        assertEquals(1, perdidos.size());
        assertEquals(StatusLembrete.PERDIDO, buscar(lembrete).getStatus());
    }

    @Test
    void naoDeveMarcarComoPerdidoAntesDoTempoLimite() {
        Lembrete lembrete = salvarLembrete(PACIENTE_ID, HORARIO);

        List<Lembrete> perdidos = servicoEm(HORARIO).verificarPerdidos(HORARIO.plusMinutes(29));

        assertTrue(perdidos.isEmpty());
        assertEquals(StatusLembrete.PENDENTE, buscar(lembrete).getStatus());
    }

    @Test
    void naoDeveAlterarLembreteJaConfirmado() {
        Lembrete lembrete = salvarLembrete(PACIENTE_ID, HORARIO);
        LembreteService service = servicoEm(HORARIO);
        service.confirmar(lembrete.getId());

        List<Lembrete> perdidos = service.verificarPerdidos(HORARIO.plusHours(2));

        assertTrue(perdidos.isEmpty());
        assertEquals(StatusLembrete.CONFIRMADO, buscar(lembrete).getStatus());
    }

    @Test
    void deveListarApenasLembretesPendentes() {
        Lembrete pendente = salvarLembrete(PACIENTE_ID, HORARIO);
        Lembrete confirmado = salvarLembrete(PACIENTE_ID, HORARIO.plusHours(8));
        salvarLembrete(99L, HORARIO);
        LembreteService service = servicoEm(HORARIO);
        service.confirmar(confirmado.getId());

        List<Lembrete> pendentes = service.listarPendentes(PACIENTE_ID);

        assertEquals(List.of(pendente.getId()), pendentes.stream().map(Lembrete::getId).toList());
    }

    @Test
    void deveLancarExcecaoParaLembreteInexistente() {
        LembreteService service = servicoEm(HORARIO);

        assertThrows(RegistroNaoEncontradoException.class, () -> service.confirmar(404L));
    }

    private LembreteService servicoEm(LocalDateTime agora) {
        Clock relogio = Clock.fixed(agora.atZone(FUSO).toInstant(), FUSO);
        HistoricoService historico = new HistoricoService(new HistoricoRepositoryMemoria(), repository, relogio);
        return new LembreteService(repository, historico, relogio, TEMPO_LIMITE);
    }

    private Lembrete salvarLembrete(Long pacienteId, LocalDateTime horario) {
        Lembrete lembrete = new Lembrete(null, pacienteId, TipoLembrete.MEDICAMENTO, 5L,
                "Losartana 50mg", horario);
        return repository.salvar(lembrete);
    }

    private Lembrete buscar(Lembrete lembrete) {
        return repository.buscarPorId(lembrete.getId()).orElseThrow();
    }
}
