package com.IntegraSaude.model;

import java.time.LocalDateTime;

// Visão da consulta usada pelo módulo de lembretes
public record ConsultaAgendada(Long id, String descricao, LocalDateTime dataHora) {
}
