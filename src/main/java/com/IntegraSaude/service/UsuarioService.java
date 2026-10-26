package com.IntegraSaude.service;

import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;

/** Regras de cadastro e consulta de usuários (RN01 e RN02). */
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final ValidadorUsuario validador;
    private final SenhaService senhaService;

    public UsuarioService(UsuarioRepository repositorio, ValidadorUsuario validador, SenhaService senhaService) {
        this.repositorio = repositorio;
        this.validador = validador;
        this.senhaService = senhaService;
    }

    // Cadastra: valida, confere e-mail duplicado, gera o hash e salva
    public Usuario cadastrar(String nome, String email, String senha, Perfil perfil) {
        String emailNormalizado = normalizarEmail(email);
        validador.validar(nome, emailNormalizado, senha, perfil);
        if (repositorio.buscarPorEmail(emailNormalizado).isPresent()) {
            throw new RegistroDuplicadoException("Ja existe um usuario com este e-mail");
        }
        String hash = senhaService.gerarHash(senha);
        return repositorio.salvar(new Usuario(nome.trim(), emailNormalizado, hash, perfil));
    }

    public Usuario buscarPorId(Long id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Usuario nao encontrado"));
    }

    public Usuario buscarPorEmail(String email) {
        return repositorio.buscarPorEmail(normalizarEmail(email))
                .orElseThrow(() -> new RegistroNaoEncontradoException("Usuario nao encontrado"));
    }

    // Usado pelo agendador para gerar lembretes de todos os pacientes
    public List<Usuario> listarPacientes() {
        return repositorio.listarPorPerfil(Perfil.PACIENTE);
    }

    // E-mail sempre sem espaços nas pontas e em minúsculas, para a unicidade funcionar
    static String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
