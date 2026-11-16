package com.IntegraSaude.model;

import java.time.LocalDateTime;

/**
 * Linha da agenda (RN14): pode ser uma dose de medicamento ou uma consulta.
 * itemId é o id do medicamento ou da consulta que originou a linha.
 */
public record ItemAgenda(LocalDateTime dataHora, TipoLembrete tipo, Long itemId, String descricao) {
}
