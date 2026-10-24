package com.IntegraSaude.service;

import com.IntegraSaude.model.MedicamentoAgendado;
import java.util.List;

// Porta para o módulo de medicamentos (Pessoa 2)
public interface FonteMedicamentos {
    List<MedicamentoAgendado> listarDoPaciente(Long pacienteId);
}
