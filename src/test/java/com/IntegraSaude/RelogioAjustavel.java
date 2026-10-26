package com.IntegraSaude;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio de teste: começa num instante fixo e pode ser adiantado, sem esperar tempo real. */
public class RelogioAjustavel extends Clock {

    private final ZoneId fuso = ZoneId.of("America/Sao_Paulo");
    private Instant instante;

    public RelogioAjustavel(LocalDateTime inicio) {
        this.instante = inicio.atZone(fuso).toInstant();
    }

    // Adianta o relógio
    public void avancar(Duration duracao) {
        instante = instante.plus(duracao);
    }

    @Override
    public ZoneId getZone() {
        return fuso;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
