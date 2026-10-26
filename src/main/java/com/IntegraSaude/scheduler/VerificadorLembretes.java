package com.IntegraSaude.scheduler;

import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.AlertaService;
import com.IntegraSaude.service.GeradorLembretes;
import com.IntegraSaude.service.LembreteService;
import com.IntegraSaude.service.Notificador;
import com.IntegraSaude.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lógica de uma rodada de verificação, separada do agendador para ser testável.
 * Recebe o "agora" por parâmetro, então os testes não precisam esperar tempo real.
 */
public class VerificadorLembretes {

    private static final Logger LOGGER = Logger.getLogger(VerificadorLembretes.class.getName());

    private final UsuarioService usuarios;
    private final GeradorLembretes gerador;
    private final LembreteService lembretes;
    private final AlertaService alertas;
    private final Notificador notificador;
    // Ids dos lembretes cujo aviso "chegou a hora" já foi mostrado (para não repetir a cada rodada)
    private final Set<Long> jaAvisados = ConcurrentHashMap.newKeySet();

    public VerificadorLembretes(UsuarioService usuarios, GeradorLembretes gerador, LembreteService lembretes,
                                AlertaService alertas, Notificador notificador) {
        this.usuarios = usuarios;
        this.gerador = gerador;
        this.lembretes = lembretes;
        this.alertas = alertas;
        this.notificador = notificador;
    }

    // Uma rodada: gera, marca perdidos e alerta, e avisa quem está na hora de agir
    public void verificar(LocalDateTime agora) {
        gerarLembretesDeHoje(agora);
        processarPerdidos(agora);
        avisarPacientes(agora);
    }

    // 1) Gera os lembretes de hoje para todos os pacientes (sem duplicar, RN12)
    private void gerarLembretesDeHoje(LocalDateTime agora) {
        for (Usuario paciente : usuarios.listarPacientes()) {
            gerador.gerarDoDia(paciente.getId(), agora.toLocalDate(), agora);
        }
    }

    // 2) Marca como PERDIDO os vencidos e dispara o alerta aos responsáveis (RN10 e RN11)
    private void processarPerdidos(LocalDateTime agora) {
        for (Lembrete perdido : lembretes.verificarPerdidos(agora)) {
            try {
                alertas.alertarResponsaveis(perdido);
            } catch (RuntimeException e) {
                // Um alerta com problema não pode impedir os demais
                LOGGER.log(Level.SEVERE, "Falha ao alertar sobre o lembrete " + perdido.getId(), e);
            }
        }
    }

    // 3) Avisa o paciente, uma vez, que o horário do lembrete chegou
    private void avisarPacientes(LocalDateTime agora) {
        for (Lembrete devido : lembretes.listarDevidos(agora)) {
            if (jaAvisados.add(devido.getId())) {
                Usuario paciente = usuarios.buscarPorId(devido.getPacienteId());
                notificador.notificar(paciente, "E hora: " + devido.getDescricao()
                        + " (confirme no menu Lembretes, codigo " + devido.getId() + ")");
            }
        }
    }
}
