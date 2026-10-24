package com.IntegraSaude.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Porta para a CalculadoraHorarios (Pessoa 2)
public interface HorariosMedicamento {
    List<LocalTime> horariosDoDia(Long medicamentoId, LocalDate dia);
}
