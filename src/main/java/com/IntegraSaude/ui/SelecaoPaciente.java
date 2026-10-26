package com.IntegraSaude.ui;

import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.service.SessaoUsuario;
import com.IntegraSaude.service.VinculoService;
import java.util.List;
import java.util.Optional;

/**
 * Descobre de qual paciente a tela vai tratar: o próprio usuário (se for paciente)
 * ou um dos pacientes vinculados (se for responsável).
 */
public class SelecaoPaciente {

    private final SessaoUsuario sessao;
    private final VinculoService vinculos;
    private final LeitorConsole leitor;
    private final SaidaConsole saida;

    public SelecaoPaciente(SessaoUsuario sessao, VinculoService vinculos, LeitorConsole leitor, SaidaConsole saida) {
        this.sessao = sessao;
        this.vinculos = vinculos;
        this.leitor = leitor;
        this.saida = saida;
    }

    public Optional<Usuario> escolher() {
        Usuario logado = sessao.exigirLogado();
        if (logado.ehPaciente()) {
            return Optional.of(logado);
        }
        List<Usuario> pacientes = vinculos.listarPacientesDoResponsavel(logado.getId());
        if (pacientes.isEmpty()) {
            saida.exibir("Voce ainda nao tem pacientes vinculados.");
            return Optional.empty();
        }
        saida.exibir("Seus pacientes:");
        for (int i = 0; i < pacientes.size(); i++) {
            saida.exibir((i + 1) + " - " + pacientes.get(i).getNome());
        }
        return Optional.of(pacientes.get(leitor.lerInteiro("Paciente:", 1, pacientes.size()) - 1));
    }
}
