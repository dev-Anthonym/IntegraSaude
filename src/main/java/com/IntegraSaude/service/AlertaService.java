package com.IntegraSaude.service;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.TipoLembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.LembreteRepository;
import com.IntegraSaude.repository.UsuarioRepository;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

// Alerta os responsáveis vinculados quando o paciente perde um lembrete (RN06 e RN07)
public class AlertaService {

    private static final Logger LOGGER = Logger.getLogger(AlertaService.class.getName());

    private final BuscaResponsaveis buscaResponsaveis;
    private final UsuarioRepository usuarioRepository;
    private final Notificador notificador;
    private final LembreteRepository lembreteRepository;
    private final HistoricoService historicoService;

    public AlertaService(BuscaResponsaveis buscaResponsaveis, UsuarioRepository usuarioRepository,
                         Notificador notificador, LembreteRepository lembreteRepository,
                         HistoricoService historicoService) {
        this.buscaResponsaveis = buscaResponsaveis;
        this.usuarioRepository = usuarioRepository;
        this.notificador = notificador;
        this.lembreteRepository = lembreteRepository;
        this.historicoService = historicoService;
    }

    public synchronized void alertar(Lembrete lembrete) {
        if (lembrete.getStatus() != StatusLembrete.PERDIDO || lembrete.isAlertaEnviado()) {
            return;
        }
        List<Usuario> responsaveis = buscaResponsaveis.listarResponsaveisDoPaciente(lembrete.getPacienteId());
        if (responsaveis.isEmpty()) {
            return;
        }
        String mensagem = montarMensagem(lembrete);
        responsaveis.forEach(responsavel -> enviar(responsavel, mensagem));
        lembrete.marcarAlertaEnviado();
        lembreteRepository.atualizar(lembrete);
        historicoService.registrar(lembrete, TipoEvento.ALERTA_ENVIADO);
    }

    // Uma falha no envio para um responsável não impede o envio para os demais
    private void enviar(Usuario responsavel, String mensagem) {
        try {
            notificador.notificar(responsavel, mensagem);
        } catch (RuntimeException e) {
            LOGGER.warning("Falha ao notificar o responsável " + responsavel.getId() + ": " + e.getMessage());
        }
    }

    private String montarMensagem(Lembrete lembrete) {
        String nomePaciente = usuarioRepository.buscarPorId(lembrete.getPacienteId())
                .map(Usuario::getNome)
                .orElse("Paciente " + lembrete.getPacienteId());
        String item = lembrete.getTipo() == TipoLembrete.MEDICAMENTO ? "o medicamento" : "a consulta";
        String horario = lembrete.getHorarioPrevisto().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return "ALERTA: " + nomePaciente + " não confirmou " + item + " " + lembrete.getDescricao()
                + " previsto para " + horario + ".";
    }
}
