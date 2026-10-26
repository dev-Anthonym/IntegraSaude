package com.IntegraSaude.service;

import com.IntegraSaude.model.Medicamento;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calcula os horários de um medicamento (RN07).
 *
 * Decisão de projeto: os horários formam uma sequência contínua que começa em
 * dataInicio + primeiroHorario e soma a frequência sem parar. Um horário que cai
 * depois da meia-noite aparece na lista do DIA SEGUINTE. Exemplo: 20:00 a cada 6h
 * gera 20:00 (dia 1) e 02:00, 08:00, 14:00, 20:00 (dia 2). A mesma lógica vale em tudo.
 */
public class CalculadoraHorarios {

    // Horários (ordenados) da sequência que caem dentro do dia informado
    public List<LocalDateTime> horariosDoDia(Medicamento medicamento, LocalDate dia) {
        LocalDateTime inicioDoDia = dia.atStartOfDay();
        LocalDateTime fimDoDia = inicioDoDia.plusDays(1);
        LocalDateTime limite = limiteDoPeriodo(medicamento);
        Duration passo = Duration.ofHours(medicamento.getFrequenciaHoras());

        List<LocalDateTime> horarios = new ArrayList<>();
        LocalDateTime atual = primeiroNaoAnteriorA(medicamento, inicioDoDia);
        // Soma a frequência enquanto estiver dentro do dia e do período do medicamento
        while (atual.isBefore(fimDoDia) && atual.isBefore(limite)) {
            horarios.add(atual);
            atual = atual.plus(passo);
        }
        return horarios;
    }

    // Primeiro horário da sequência que não é anterior ao momento informado (vazio se o período acabou)
    public Optional<LocalDateTime> proximoHorario(Medicamento medicamento, LocalDateTime aPartirDe) {
        LocalDateTime candidato = primeiroNaoAnteriorA(medicamento, aPartirDe);
        return candidato.isBefore(limiteDoPeriodo(medicamento)) ? Optional.of(candidato) : Optional.empty();
    }

    // O período termina no fim do dia da dataFim (limite exclusivo = dia seguinte à 00:00)
    private LocalDateTime limiteDoPeriodo(Medicamento medicamento) {
        return medicamento.getDataFim().plusDays(1).atStartOfDay();
    }

    // Pula direto para o primeiro múltiplo da frequência que é >= referência (sem laço longo)
    private LocalDateTime primeiroNaoAnteriorA(Medicamento medicamento, LocalDateTime referencia) {
        LocalDateTime inicio = medicamento.getDataInicio().atTime(medicamento.getPrimeiroHorario());
        if (!referencia.isAfter(inicio)) {
            return inicio;
        }
        long passoSegundos = Duration.ofHours(medicamento.getFrequenciaHoras()).toSeconds();
        long decorridoSegundos = Duration.between(inicio, referencia).toSeconds();
        long passosNecessarios = (decorridoSegundos + passoSegundos - 1) / passoSegundos;
        return inicio.plusSeconds(passosNecessarios * passoSegundos);
    }
}
