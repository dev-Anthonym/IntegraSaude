package com.IntegraSaude.service;

import com.IntegraSaude.model.ConsultaAgendada;
import java.util.List;

// Porta para o módulo de consultas (Pessoa 3)
public interface FonteConsultas {
    List<ConsultaAgendada> listarDoPaciente(Long pacienteId);
}
