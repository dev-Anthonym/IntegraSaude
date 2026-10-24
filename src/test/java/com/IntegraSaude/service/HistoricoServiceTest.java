package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.IntegraSaude.model.EventoHistorico;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.MedicamentoAgendado;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HistoricoServiceTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 24, 8, 0);
    private static final Duration TEMPO_LIMITE = Duration.ofMinutes(30);
    private static final Long OUTRO_PACIENTE_ID = 99L;

    private final RelogioAjustavel relogio = new RelogioAjustavel(HORARIO);
    private LembreteRepository lembreteRepository;
    private UsuarioRepository usuarioRepository;
    private HistoricoService historicoService;
    private LembreteService lembreteService;
    private Usuario paciente;

    @BeforeEach
    void setUp() {
        lembreteRepository = new LembreteRepositoryMemoria();
        usuarioRepository = new UsuarioRepositoryMemoria();
        historicoService = new HistoricoService(new HistoricoRepositoryMemoria(), lembreteRepository, relogio);
        lembreteService = new LembreteService(lembreteRepository, historicoService, relogio, TEMPO_LIMITE);
        paciente = salvarUsuario("Ana Maria", Perfil.PACIENTE);
    }

    @Test
    void deveRegistrarEventoAoGerarLembrete() {
        GeradorLembretes gerador = new GeradorLembretes(
                pacienteId -> List.of(new MedicamentoAgendado(5L, "Losartana 50mg", HORARIO.toLocalDate(), null)),
                (medicamentoId, dia) -> List.of(LocalTime.of(8, 0)),
                pacienteId -> List.of(),
                lembreteRepository, historicoService, Duration.ofMinutes(60));

        gerador.gerarDoDia(paciente.getId(), HORARIO.toLocalDate());

        assertEquals(List.of(TipoEvento.GERADO), tiposDoHistorico(paciente.getId()));
    }

    @Test
    void deveRegistrarEventoAoConfirmarLembrete() {
        Lembrete lembrete = salvarLembrete(paciente.getId());
        relogio.ajustarPara(HORARIO.plusMinutes(5));

        lembreteService.confirmar(lembrete.getId());

        List<EventoHistorico> eventos = historicoService.listarDoPaciente(paciente.getId());
        assertEquals(1, eventos.size());
        assertEquals(TipoEvento.CONFIRMADO, eventos.get(0).getTipoEvento());
        assertEquals(lembrete.getId(), eventos.get(0).getLembreteId());
        assertEquals(HORARIO.plusMinutes(5), eventos.get(0).getDataHora());
    }

    @Test
    void deveRegistrarEventoAoMarcarComoPerdido() {
        salvarLembrete(paciente.getId());

        lembreteService.verificarPerdidos(HORARIO.plusMinutes(31));

        assertEquals(List.of(TipoEvento.PERDIDO), tiposDoHistorico(paciente.getId()));
    }

    @Test
    void deveRegistrarEventoQuandoAlertaForEnviado() {
        Usuario responsavel = salvarUsuario("Carlos", Perfil.RESPONSAVEL);
        AlertaService alertaService = new AlertaService(pacienteId -> List.of(responsavel),
                usuarioRepository, new NotificadorFalso(), lembreteRepository, historicoService);
        salvarLembrete(paciente.getId());

        lembreteService.verificarPerdidos(HORARIO.plusMinutes(31)).forEach(alertaService::alertar);

        assertEquals(List.of(TipoEvento.PERDIDO, TipoEvento.ALERTA_ENVIADO), tiposDoHistorico(paciente.getId()));
    }

    @Test
    void deveListarHistoricoDoPacienteOrdenado() {
        Lembrete primeiro = salvarLembrete(paciente.getId());
        Lembrete segundo = salvarLembrete(paciente.getId());
        Lembrete deOutroPaciente = salvarLembrete(OUTRO_PACIENTE_ID);
        registrarEm(HORARIO.plusHours(2), segundo, TipoEvento.CONFIRMADO);
        registrarEm(HORARIO, primeiro, TipoEvento.GERADO);
        registrarEm(HORARIO.plusHours(1), deOutroPaciente, TipoEvento.GERADO);
        registrarEm(HORARIO.plusHours(1), primeiro, TipoEvento.CONFIRMADO);

        List<EventoHistorico> eventos = historicoService.listarDoPaciente(paciente.getId());

        assertEquals(List.of(HORARIO, HORARIO.plusHours(1), HORARIO.plusHours(2)),
                eventos.stream().map(EventoHistorico::getDataHora).toList());
    }

    @Test
    void deveFiltrarHistoricoPorPeriodo() {
        Lembrete lembrete = salvarLembrete(paciente.getId());
        registrarEm(HORARIO.minusDays(1), lembrete, TipoEvento.GERADO);
        registrarEm(HORARIO, lembrete, TipoEvento.GERADO);
        registrarEm(HORARIO.plusDays(1), lembrete, TipoEvento.CONFIRMADO);
        LocalDate dia = HORARIO.toLocalDate();

        List<EventoHistorico> eventos = historicoService.listarDoPacienteNoPeriodo(paciente.getId(),
                dia.atStartOfDay(), dia.atTime(LocalTime.MAX));

        assertEquals(List.of(HORARIO), eventos.stream().map(EventoHistorico::getDataHora).toList());
    }

    private void registrarEm(LocalDateTime momento, Lembrete lembrete, TipoEvento tipo) {
        relogio.ajustarPara(momento);
        historicoService.registrar(lembrete, tipo);
    }

    private List<TipoEvento> tiposDoHistorico(Long pacienteId) {
        return historicoService.listarDoPaciente(pacienteId).stream()
                .map(EventoHistorico::getTipoEvento)
                .toList();
    }

    private Usuario salvarUsuario(String nome, Perfil perfil) {
        String email = nome.toLowerCase().replace(" ", ".") + "@email.com";
        return usuarioRepository.salvar(new Usuario(null, nome, email, "hash", perfil));
    }

    private Lembrete salvarLembrete(Long pacienteId) {
        return lembreteRepository.salvar(new Lembrete(null, pacienteId,
                TipoLembrete.MEDICAMENTO, 5L, "Losartana 50mg", HORARIO));
    }
}
