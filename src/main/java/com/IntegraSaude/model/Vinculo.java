package com.IntegraSaude.model;

/** Liga um paciente a um responsável (RN03). Um paciente pode ter vários responsáveis e vice-versa. */
public class Vinculo extends EntidadeBase {

    private final Long pacienteId;
    private final Long responsavelId;

    public Vinculo(Long pacienteId, Long responsavelId) {
        this.pacienteId = pacienteId;
        this.responsavelId = responsavelId;
    }

    public Long getPacienteId() {
        return pacienteId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }
}
