package com.IntegraSaude.model;

import com.IntegraSaude.exception.OperacaoInvalidaException;
import java.time.LocalDateTime;

/**
 * Consulta médica de um paciente (RN06 e RN08). A especialidade vem do médico.
 * Consulta cancelada não é apagada: muda para o status CANCELADA (como no banco).
 */
public class Consulta extends EntidadeBase {

    private final Long pacienteId;
    private final Medico medico;
    private final String local;
    private final LocalDateTime dataHora;
    private StatusConsulta status;

    // Consulta nova: nasce AGENDADA
    public Consulta(Long pacienteId, Medico medico, String local, LocalDateTime dataHora) {
        this(pacienteId, medico, local, dataHora, StatusConsulta.AGENDADA);
    }

    // Reconstrói uma consulta já salva (usado pelos repositórios JDBC)
    public Consulta(Long pacienteId, Medico medico, String local, LocalDateTime dataHora, StatusConsulta status) {
        this.pacienteId = pacienteId;
        this.medico = medico;
        this.local = local;
        this.dataHora = dataHora;
        this.status = status;
    }

    // Transição AGENDADA -> CANCELADA
    public void cancelar() {
        if (status != StatusConsulta.AGENDADA) {
            throw new OperacaoInvalidaException("Apenas consultas agendadas podem ser canceladas");
        }
        this.status = StatusConsulta.CANCELADA;
    }

    public boolean estaAgendada() {
        return status == StatusConsulta.AGENDADA;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Medico getMedico() {
        return medico;
    }

    public String getLocal() {
        return local;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public StatusConsulta getStatus() {
        return status;
    }
}
