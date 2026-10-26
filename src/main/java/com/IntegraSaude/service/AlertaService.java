package com.IntegraSaude.service;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.model.DescricaoLembrete;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.StatusLembrete;
import com.IntegraSaude.model.TipoEvento;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.LembreteRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Avisa os responsáveis quando um lembrete é perdido, uma única vez (RN11). */
public class AlertaService {

    private static final Logger LOGGER = Logger.getLogger(AlertaService.class.getName());

    private final LembreteRepository lembretes;
    private final VinculoService vinculos;
    private final UsuarioService usuarios;
    private final HistoricoService historico;
    private final Notificador notificador;

    public AlertaService(LembreteRepository lembretes, VinculoService vinculos, UsuarioService usuarios,
                         HistoricoService historico, Notificador notificador) {
        this.lembretes = lembretes;
        this.vinculos = vinculos;
        this.usuarios = usuarios;
        this.historico = historico;
        this.notificador = notificador;
    }

    // Notifica todos os responsáveis do paciente e devolve quantos receberam o aviso
    public int alertarResponsaveis(Lembrete lembrete) {
        // Só lembretes perdidos geram alerta
        if (lembrete.getStatus() != StatusLembrete.PERDIDO) {
            throw new OperacaoInvalidaException("Apenas lembretes perdidos geram alerta");
        }
        // Alerta uma única vez por lembrete (coluna alerta_enviado)
        if (lembrete.isAlertaEnviado()) {
            return 0;
        }
        // Sem vínculo: não alerta e não dá erro
        List<Usuario> responsaveis = vinculos.listarResponsaveisDoPaciente(lembrete.getPacienteId());
        if (responsaveis.isEmpty()) {
            return 0;
        }
        String mensagem = montarMensagem(lembrete);
        List<String> avisados = enviarParaTodos(responsaveis, mensagem);
        // Marca o alerta como enviado e registra o evento só se pelo menos um aviso foi entregue
        if (!avisados.isEmpty()) {
            lembrete.marcarAlertaEnviado();
            lembretes.atualizar(lembrete);
            historico.registrar(lembrete.getPacienteId(), lembrete.getId(), TipoEvento.ALERTA_ENVIADO,
                    DescricaoLembrete.evento(TipoEvento.ALERTA_ENVIADO, lembrete.getDescricao()));
        }
        return avisados.size();
    }

    // Envia para cada responsável; se um falhar, registra em log e continua com os demais
    private List<String> enviarParaTodos(List<Usuario> responsaveis, String mensagem) {
        List<String> avisados = new ArrayList<>();
        for (Usuario responsavel : responsaveis) {
            try {
                notificador.notificar(responsavel, mensagem);
                avisados.add(responsavel.getNome());
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Falha ao notificar o responsavel " + responsavel.getId(), e);
            }
        }
        return avisados;
    }

    private String montarMensagem(Lembrete lembrete) {
        String paciente = usuarios.buscarPorId(lembrete.getPacienteId()).getNome();
        return "ALERTA: " + paciente + " nao confirmou o lembrete (" + lembrete.getDescricao() + ")";
    }
}
