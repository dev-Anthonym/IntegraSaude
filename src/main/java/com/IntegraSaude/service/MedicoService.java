package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.repository.EspecialidadeRepository;
import com.IntegraSaude.repository.MedicoRepository;
import java.util.List;

/** Cadastro simples de médicos (cada médico pertence a uma especialidade). */
public class MedicoService {

    private final MedicoRepository medicos;
    private final EspecialidadeRepository especialidades;

    public MedicoService(MedicoRepository medicos, EspecialidadeRepository especialidades) {
        this.medicos = medicos;
        this.especialidades = especialidades;
    }

    public Medico cadastrar(String nome, Long especialidadeId) {
        if (nome == null || nome.isBlank()) {
            throw new DadoInvalidoException("O nome do medico e obrigatorio");
        }
        Especialidade especialidade = especialidades.buscarPorId(especialidadeId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Especialidade nao encontrada"));
        return medicos.salvar(new Medico(nome.trim(), especialidade));
    }

    public List<Medico> listarPorEspecialidade(Long especialidadeId) {
        return medicos.listarPorEspecialidade(especialidadeId);
    }

    public Medico buscarPorId(Long id) {
        return medicos.buscarPorId(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Medico nao encontrado"));
    }
}
