package com.IntegraSaude.model;

import java.time.LocalDateTime;

/** Linha da agenda (RN14): pode ser uma dose de medicamento ou uma consulta. */
public record ItemAgenda(LocalDateTime dataHora, TipoLembrete tipo, String descricao) {
}
