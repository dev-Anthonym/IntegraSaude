package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlertaServiceTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 24, 8, 0);

    private final NotificadorFalso notificador = new NotificadorFalso();
    private final List<Usuario> responsaveis = new ArrayList<>();
    private UsuarioRepository usuarioRepository;
    private LembreteRepository lembreteRepository;
    private AlertaService alertaService;
    private Usuario paciente;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryMemoria();
        lembreteRepository = new LembreteRepositoryMemoria();
        paciente = salvarUsuario("Ana Maria", Perfil.PACIENTE);
        HistoricoService historico = new HistoricoService(new HistoricoRepositoryMemoria(),
                lembreteRepository, Clock.systemDefaultZone());
        alertaService = new AlertaService(pacienteId -> responsaveis, usuarioRepository,
                notificador, lembreteRepository, historico);
    }

    @Test
    void deveAlertarTodosOsResponsaveisDoPaciente() {
        Usuario carlos = adicionarResponsavel("Carlos");
        Usuario beatriz = adicionarResponsavel("Beatriz");
        Lembrete lembrete = lembretePerdido();

        alertaService.alertar(lembrete);

        assertEquals(List.of(carlos, beatriz), notificador.destinatarios());
        assertTrue(lembreteRepository.buscarPorId(lembrete.getId()).orElseThrow().isAlertaEnviado());
    }

    @Test
    void naoDeveAlertarQuandoNaoHaVinculo() {
        Lembrete lembrete = lembretePerdido();

        assertDoesNotThrow(() -> alertaService.alertar(lembrete));
        assertTrue(notificador.envios().isEmpty());
    }

    @Test
    void naoDeveAlertarDuasVezesPeloMesmoLembrete() {
        adicionarResponsavel("Carlos");
        Lembrete lembrete = lembretePerdido();

        alertaService.alertar(lembrete);
        alertaService.alertar(lembrete);

        assertEquals(1, notificador.envios().size());
    }

    @Test
    void naoDeveAlertarLembreteQueNaoEstaPerdido() {
        adicionarResponsavel("Carlos");
        Lembrete lembrete = salvarLembrete();

        alertaService.alertar(lembrete);

        assertTrue(notificador.envios().isEmpty());
    }

    @Test
    void deveIncluirNomeDoPacienteEMedicamentoNaMensagem() {
        adicionarResponsavel("Carlos");

        alertaService.alertar(lembretePerdido());

        String mensagem = notificador.envios().get(0).mensagem();
        assertTrue(mensagem.contains("Ana Maria"), mensagem);
        assertTrue(mensagem.contains("Losartana 50mg"), mensagem);
        assertTrue(mensagem.contains("24/10/2026 08:00"), mensagem);
    }

    @Test
    void deveContinuarNotificandoSeUmEnvioFalhar() {
        Usuario carlos = adicionarResponsavel("Carlos");
        Usuario beatriz = adicionarResponsavel("Beatriz");
        notificador.falharPara(carlos);

        assertDoesNotThrow(() -> alertaService.alertar(lembretePerdido()));

        assertEquals(List.of(beatriz), notificador.destinatarios());
    }

    private Usuario adicionarResponsavel(String nome) {
        Usuario responsavel = salvarUsuario(nome, Perfil.RESPONSAVEL);
        responsaveis.add(responsavel);
        return responsavel;
    }

    private Usuario salvarUsuario(String nome, Perfil perfil) {
        String email = nome.toLowerCase().replace(" ", ".") + "@email.com";
        return usuarioRepository.salvar(new Usuario(null, nome, email, "hash", perfil));
    }

    private Lembrete lembretePerdido() {
        Lembrete lembrete = salvarLembrete();
        lembrete.marcarComoPerdido();
        return lembrete;
    }

    private Lembrete salvarLembrete() {
        return lembreteRepository.salvar(new Lembrete(null, paciente.getId(),
                TipoLembrete.MEDICAMENTO, 5L, "Losartana 50mg", HORARIO));
    }
}
