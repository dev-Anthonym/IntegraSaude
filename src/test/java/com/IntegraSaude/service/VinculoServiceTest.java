package com.IntegraSaude.service;

import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import com.IntegraSaude.repository.UsuarioRepositoryMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VinculoServiceTest {

    private UsuarioRepository usuarioRepository;
    private UsuarioService usuarioService;
    private VinculoService vinculoService;

    private Usuario paciente;
    private Usuario responsavel;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryMemoria();
        usuarioService = new UsuarioService(usuarioRepository);
        vinculoService = new VinculoService(usuarioRepository);

        // Criação de utilizadores para os testes de vínculo
        paciente = usuarioService.cadastrar("Paciente Teste", "paciente@email.com", "Senha123", Perfil.PACIENTE);
        responsavel = usuarioService.cadastrar("Responsavel Teste", "responsavel@email.com", "Senha123", Perfil.RESPONSAVEL);
    }

    @Test
    void deveVincularResponsavelAoPacienteComSucesso() {
        // Realiza o vínculo entre o responsável e o paciente
        vinculoService.vincular(responsavel.getId(), paciente.getId());

        // Verifica se o paciente agora tem o ID do responsável associado
        Usuario pacienteAtualizado = usuarioRepository.buscarPorId(paciente.getId()).get();
        assertEquals(responsavel.getId(), pacienteAtualizado.getResponsavelId());
    }

    @Test
    void deveFalharAoVincularPerfilInvalidoComoResponsavel() {
        // Tenta vincular um paciente como se fosse responsável (deve lançar exceção)
        Usuario outroPaciente = usuarioService.cadastrar("Outro Paciente", "outro@email.com", "Senha123", Perfil.PACIENTE);

        assertThrows(DadoInvalidoException.class, () -> {
            vinculoService.vincular(outroPaciente.getId(), paciente.getId());
        });
    }
}