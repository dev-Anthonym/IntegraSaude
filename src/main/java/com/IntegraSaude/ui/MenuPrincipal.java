package com.IntegraSaude.ui;

import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.SessaoUsuario;
import java.util.ArrayList;
import java.util.List;

/** Menu inicial. As opções mudam conforme quem está logado (nenhuma, paciente ou responsável). */
public class MenuPrincipal extends MenuBase {

    private final SessaoUsuario sessao;
    private final MenuUsuarios menuUsuarios;
    private final MenuMedicamentos menuMedicamentos;
    private final MenuConsultas menuConsultas;
    private final MenuLembretes menuLembretes;
    private final MenuAgenda menuAgenda;
    private final DadosDemonstracao demonstracao;

    public MenuPrincipal(LeitorConsole leitor, SaidaConsole saida, SessaoUsuario sessao,
                         MenuUsuarios menuUsuarios, MenuMedicamentos menuMedicamentos,
                         MenuConsultas menuConsultas, MenuLembretes menuLembretes,
                         MenuAgenda menuAgenda, DadosDemonstracao demonstracao) {
        super(leitor, saida);
        this.sessao = sessao;
        this.menuUsuarios = menuUsuarios;
        this.menuMedicamentos = menuMedicamentos;
        this.menuConsultas = menuConsultas;
        this.menuLembretes = menuLembretes;
        this.menuAgenda = menuAgenda;
        this.demonstracao = demonstracao;
    }

    // Abre o menu e só volta quando o usuário escolhe Sair
    public void executar() {
        executarMenu(this::titulo, "Sair", this::opcoesDisponiveis);
    }

    private String titulo() {
        return sessao.usuario()
                .map(u -> "Lembretes de Medicamentos e Consultas | " + u.getNome() + " (" + u.getPerfil() + ")")
                .orElse("Lembretes de Medicamentos e Consultas | sem login");
    }

    // Decide quais opções mostrar para o perfil logado
    private List<Opcao> opcoesDisponiveis() {
        List<Opcao> opcoes = new ArrayList<>();
        Usuario logado = sessao.usuario().orElse(null);
        if (logado == null) {
            opcoes.add(new Opcao("Cadastrar usuario", menuUsuarios::cadastrar));
            opcoes.add(new Opcao("Fazer login", menuUsuarios::login));
            opcoes.add(new Opcao("Carregar dados de demonstracao",
                    () -> saida.exibir(demonstracao.carregar())));
            return opcoes;
        }
        if (logado.ehPaciente()) {
            opcoes.add(new Opcao("Medicamentos", menuMedicamentos::executar));
            opcoes.add(new Opcao("Consultas", menuConsultas::executar));
            opcoes.add(new Opcao("Lembretes e historico", menuLembretes::executar));
            opcoes.add(new Opcao("Agenda", menuAgenda::executar));
            opcoes.add(new Opcao("Meus responsaveis", menuUsuarios::menuVinculos));
        } else {
            opcoes.add(new Opcao("Meus pacientes", menuUsuarios::menuVinculos));
            opcoes.add(new Opcao("Lembretes e historico dos pacientes", menuLembretes::executar));
            opcoes.add(new Opcao("Agenda dos pacientes", menuAgenda::executar));
        }
        opcoes.add(new Opcao("Sair da conta (logout)", menuUsuarios::logout));
        return opcoes;
    }
}
