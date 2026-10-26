package com.IntegraSaude.ui;

import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.AutenticacaoService;
import com.IntegraSaude.service.SessaoUsuario;
import com.IntegraSaude.service.UsuarioService;
import com.IntegraSaude.service.VinculoService;
import java.util.List;

/** Cadastro, login, logout e vínculos. Só lê dados, chama um serviço e mostra o resultado. */
public class MenuUsuarios extends MenuBase {

    private final UsuarioService usuarios;
    private final AutenticacaoService autenticacao;
    private final VinculoService vinculos;
    private final SessaoUsuario sessao;

    public MenuUsuarios(LeitorConsole leitor, SaidaConsole saida, UsuarioService usuarios,
                        AutenticacaoService autenticacao, VinculoService vinculos, SessaoUsuario sessao) {
        super(leitor, saida);
        this.usuarios = usuarios;
        this.autenticacao = autenticacao;
        this.vinculos = vinculos;
        this.sessao = sessao;
    }

    // Cadastro de paciente ou responsável
    public void cadastrar() {
        String nome = leitor.lerTexto("Nome:");
        String email = leitor.lerTexto("E-mail:");
        String senha = leitor.lerTexto("Senha (minimo 8 caracteres, com letra e numero):");
        saida.exibir("Perfil: 1 - Paciente | 2 - Responsavel");
        Perfil perfil = leitor.lerInteiro("Perfil:", 1, 2) == 1 ? Perfil.PACIENTE : Perfil.RESPONSAVEL;
        usuarios.cadastrar(nome, email, senha, perfil);
        saida.exibir("Usuario cadastrado com sucesso.");
    }

    // Login: guarda o usuário na sessão
    public void login() {
        String email = leitor.lerTexto("E-mail:");
        String senha = leitor.lerTexto("Senha:");
        Usuario usuario = autenticacao.login(email, senha);
        sessao.login(usuario);
        saida.exibir("Bem-vindo(a), " + usuario.getNome() + "!");
    }

    public void logout() {
        sessao.logout();
        saida.exibir("Sessao encerrada.");
    }

    // Submenu de vínculos (paciente vê responsáveis; responsável vê pacientes)
    public void menuVinculos() {
        Usuario logado = sessao.exigirLogado();
        String rotulo = logado.ehPaciente() ? "responsavel" : "paciente";
        executarMenu(() -> "Vinculos", "Voltar", () -> List.of(
                new Opcao("Listar meus " + rotulo + "s", this::listarVinculos),
                new Opcao("Adicionar " + rotulo + " (pelo e-mail)", this::adicionarVinculo),
                new Opcao("Remover " + rotulo + " (pelo e-mail)", this::removerVinculo)));
    }

    private void listarVinculos() {
        Usuario logado = sessao.exigirLogado();
        List<Usuario> lista = logado.ehPaciente()
                ? vinculos.listarResponsaveisDoPaciente(logado.getId())
                : vinculos.listarPacientesDoResponsavel(logado.getId());
        if (lista.isEmpty()) {
            saida.exibir("Nenhum vinculo cadastrado.");
        }
        lista.forEach(u -> saida.exibir("- " + u.getNome() + " <" + u.getEmail() + ">"));
    }

    private void adicionarVinculo() {
        Usuario logado = sessao.exigirLogado();
        Usuario outro = usuarios.buscarPorEmail(leitor.lerTexto("E-mail da outra pessoa:"));
        if (logado.ehPaciente()) {
            vinculos.criar(logado.getId(), outro.getId());
        } else {
            vinculos.criar(outro.getId(), logado.getId());
        }
        saida.exibir("Vinculo criado com " + outro.getNome() + ".");
    }

    private void removerVinculo() {
        Usuario logado = sessao.exigirLogado();
        Usuario outro = usuarios.buscarPorEmail(leitor.lerTexto("E-mail da outra pessoa:"));
        if (logado.ehPaciente()) {
            vinculos.remover(logado.getId(), outro.getId());
        } else {
            vinculos.remover(outro.getId(), logado.getId());
        }
        saida.exibir("Vinculo removido.");
    }
}
