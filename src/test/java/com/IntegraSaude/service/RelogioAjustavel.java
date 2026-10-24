package com.IntegraSaude.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

// Relógio de teste que permite avançar o tempo sem esperar
class RelogioAjustavel extends Clock {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private Instant instante;

    RelogioAjustavel(LocalDateTime momento) {
        ajustarPara(momento);
    }

    void ajustarPara(LocalDateTime momento) {
        this.instante = momento.atZone(FUSO).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return FUSO;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return Clock.fixed(instante, zona);
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
