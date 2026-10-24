package com.IntegraSaude.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponsaveisPorVinculoTest {

    private UsuarioRepository usuarioRepository;
    private VinculoService vinculoService;
    private ResponsaveisPorVinculo buscaResponsaveis;
    private Usuario paciente;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryMemoria();
        vinculoService = new VinculoService(usuarioRepository);
        buscaResponsaveis = new ResponsaveisPorVinculo(usuarioRepository);
        paciente = salvar("Ana Maria", "ana@email.com", Perfil.PACIENTE);
    }

    @Test
    void deveListarResponsavelVinculadoAoPaciente() {
        Usuario responsavel = salvar("Carlos", "carlos@email.com", Perfil.RESPONSAVEL);
        vinculoService.vincular(responsavel.getId(), paciente.getId());

        List<Usuario> responsaveis = buscaResponsaveis.listarResponsaveisDoPaciente(paciente.getId());

        assertEquals(List.of(responsavel), responsaveis);
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHaVinculo() {
        assertTrue(buscaResponsaveis.listarResponsaveisDoPaciente(paciente.getId()).isEmpty());
    }

    @Test
    void deveRetornarListaVaziaParaPacienteInexistente() {
        assertTrue(buscaResponsaveis.listarResponsaveisDoPaciente(404L).isEmpty());
    }

    private Usuario salvar(String nome, String email, Perfil perfil) {
        return usuarioRepository.salvar(new Usuario(null, nome, email, "hash", perfil));
    }
}
