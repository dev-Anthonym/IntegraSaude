package com.IntegraSaude.service;

import com.IntegraSaude.exception.CredenciaisInvalidasException;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.UsuarioRepository;

/** Faz o login (RN04): a falha nunca revela se o erro foi no e-mail ou na senha. */
public class AutenticacaoService {

    public static final String MENSAGEM_FALHA = "E-mail ou senha invalidos";

    private final UsuarioRepository repositorio;
    private final SenhaService senhaService;
    // Hash falso: permite gastar o mesmo tempo mesmo quando o e-mail não existe
    private final String hashFalso;

    public AutenticacaoService(UsuarioRepository repositorio, SenhaService senhaService) {
        this.repositorio = repositorio;
        this.senhaService = senhaService;
        this.hashFalso = senhaService.gerarHash("senha-falsa-1");
    }

    // Devolve o usuário ou lança CredenciaisInvalidasException com mensagem genérica
    public Usuario login(String email, String senha) {
        Usuario usuario = repositorio.buscarPorEmail(UsuarioService.normalizarEmail(email)).orElse(null);
        String hashParaConferir = usuario == null ? hashFalso : usuario.getSenhaHash();
        boolean senhaConfere = senhaService.conferir(senha, hashParaConferir);
        if (usuario == null || !senhaConfere) {
            throw new CredenciaisInvalidasException(MENSAGEM_FALHA);
        }
        return usuario;
    }
}
