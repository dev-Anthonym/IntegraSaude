package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.HistoricoRepositoryMemoria;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.LembreteRepositoryMemoria;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VerificadorLembretesTest {

    private static final LocalDateTime HORARIO = LocalDateTime.of(2026, 10, 24, 8, 0);
    private static final Duration TEMPO_LIMITE = Duration.ofMinutes(30);

    private final NotificadorFalso notificador = new NotificadorFalso();
    private LembreteRepository lembreteRepository;
    private UsuarioRepository usuarioRepository;
    private VerificadorLembretes verificador;
    private Usuario paciente;
    private Usuario responsavel;

    @BeforeEach
    void setUp() {
        lembreteRepository = new LembreteRepositoryMemoria();
        usuarioRepository = new UsuarioRepositoryMemoria();
        paciente = salvarUsuario("Ana Maria", Perfil.PACIENTE);
        responsavel = salvarUsuario("Carlos", Perfil.RESPONSAVEL);
        HistoricoService historico = new HistoricoService(new HistoricoRepositoryMemoria(),
                lembreteRepository, Clock.systemDefaultZone());
        LembreteService lembreteService = new LembreteService(lembreteRepository, historico,
                Clock.systemDefaultZone(), TEMPO_LIMITE);
        AlertaService alertaService = new AlertaService(pacienteId -> List.of(responsavel),
                usuarioRepository, notificador, lembreteRepository, historico);
        verificador = new VerificadorLembretes(lembreteService, alertaService, usuarioRepository, notificador);
    }

    @Test
    void deveVerificarLembretesVencidosNoMomentoInformado() {
        Lembrete lembrete = salvarLembrete();

        verificador.verificar(HORARIO.plusMinutes(31));

        assertEquals(StatusLembrete.PERDIDO, lembrete.getStatus());
    }

    @Test
    void deveDispararAlertaParaLembreteVencido() {
        salvarLembrete();

        verificador.verificar(HORARIO.plusMinutes(31));

        List<NotificadorFalso.Envio> alertas = notificador.enviosPara(responsavel);
        assertEquals(1, alertas.size());
        assertTrue(alertas.get(0).mensagem().contains("ALERTA"));
    }

    @Test
    void deveAvisarPacienteNoHorarioDoLembrete() {
        salvarLembrete();

        verificador.verificar(HORARIO);

        List<NotificadorFalso.Envio> avisos = notificador.enviosPara(paciente);
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).mensagem().contains("Losartana 50mg"));
    }

    @Test
    void naoDeveAvisarPacienteAntesDoHorario() {
        salvarLembrete();

        verificador.verificar(HORARIO.minusMinutes(1));

        assertTrue(notificador.envios().isEmpty());
    }

    @Test
    void naoDeveAvisarPacienteDuasVezesPeloMesmoLembrete() {
        salvarLembrete();

        verificador.verificar(HORARIO);
        verificador.verificar(HORARIO.plusMinutes(1));

        assertEquals(1, notificador.enviosPara(paciente).size());
    }

    @Test
    void deveContinuarAvisandoOutrosPacientesSeUmEnvioFalhar() {
        Usuario outroPaciente = salvarUsuario("Bruno Lima", Perfil.PACIENTE);
        salvarLembrete();
        lembreteRepository.salvar(new Lembrete(null, outroPaciente.getId(),
                TipoLembrete.MEDICAMENTO, 6L, "Metformina 850mg", HORARIO));
        notificador.falharPara(paciente);

        assertDoesNotThrow(() -> verificador.verificar(HORARIO));

        assertEquals(1, notificador.enviosPara(outroPaciente).size());
    }

    private Usuario salvarUsuario(String nome, Perfil perfil) {
        String email = nome.toLowerCase().replace(" ", ".") + "@email.com";
        return usuarioRepository.salvar(new Usuario(null, nome, email, "hash", perfil));
    }

    private Lembrete salvarLembrete() {
        return lembreteRepository.salvar(new Lembrete(null, paciente.getId(),
                TipoLembrete.MEDICAMENTO, 5L, "Losartana 50mg", HORARIO));
    }
}
