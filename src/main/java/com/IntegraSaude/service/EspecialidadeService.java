package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.repository.EspecialidadeRepository;
import java.util.List;

/** Cadastro simples de especialidades médicas. */
public class EspecialidadeService {

    private final EspecialidadeRepository repositorio;

    public EspecialidadeService(EspecialidadeRepository repositorio) {
        this.repositorio = repositorio;
    }

    public Especialidade cadastrar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome da especialidade e obrigatorio");
        }
        boolean existe = repositorio.listar().stream().anyMatch(e -> e.getNome().equalsIgnoreCase(nome.trim()));
        if (existe) {
            throw new RegistroDuplicadoException("Especialidade ja cadastrada");
        }
        return repositorio.salvar(new Especialidade(nome.trim()));
    }

    public List<Especialidade> listar() {
        return repositorio.listar();
    }

    public Especialidade buscarPorId(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Especialidade nao encontrada"));
    }
}
