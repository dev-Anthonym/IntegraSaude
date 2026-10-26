package com.IntegraSaude.repository;

import com.IntegraSaude.model.EntidadeBase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * "Tabela" em memória: um Map de id para entidade e um contador de id.
 * Todos os métodos são synchronized porque o agendador roda em outra thread.
 */
final class TabelaMemoria<T extends EntidadeBase> {

    private final Map<Long, T> linhas = new LinkedHashMap<>();
    private long proximoId = 1;

    // Gera o id (como o AUTOINCREMENT do banco) e guarda a entidade
    synchronized T inserir(T entidade) {
        entidade.setId(proximoId++);
        linhas.put(entidade.getId(), entidade);
        return entidade;
    }

    // Substitui a linha que tem o mesmo id
    synchronized void substituir(T entidade) {
        if (entidade.getId() != null && linhas.containsKey(entidade.getId())) {
            linhas.put(entidade.getId(), entidade);
        }
    }

    synchronized Optional<T> buscar(Long id) {
        return Optional.ofNullable(linhas.get(id));
    }

    // Devolve uma cópia da lista filtrada (evita ConcurrentModificationException)
    synchronized List<T> filtrar(Predicate<T> criterio) {
        List<T> resultado = new ArrayList<>();
        for (T linha : linhas.values()) {
            if (criterio.test(linha)) {
                resultado.add(linha);
            }
        }
        return resultado;
    }

    synchronized boolean remover(Long id) {
        return linhas.remove(id) != null;
    }

    // Remove todas as linhas que atendem ao critério
    synchronized void removerSe(Predicate<T> criterio) {
        linhas.values().removeIf(criterio);
    }

    // Permite verificar uma regra de unicidade e inserir de forma atômica
    synchronized boolean existe(Predicate<T> criterio) {
        return linhas.values().stream().anyMatch(criterio);
    }
}
