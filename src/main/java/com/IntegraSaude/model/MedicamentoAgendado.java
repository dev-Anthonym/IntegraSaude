package com.IntegraSaude.model;

import java.time.LocalDate;

// Visão do medicamento usada pelo módulo de lembretes (fim nulo significa uso contínuo)
public record MedicamentoAgendado(Long id, String nome, LocalDate inicio, LocalDate fim) {

    public boolean estaAtivoEm(LocalDate dia) {
        boolean comecou = !dia.isBefore(inicio);
        boolean terminou = fim != null && dia.isAfter(fim);
        return comecou && !terminou;
    }
}
