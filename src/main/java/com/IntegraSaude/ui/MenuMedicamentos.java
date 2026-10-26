package com.IntegraSaude.ui;

import com.IntegraSaude.model.Medicamento;
import com.IntegraSaude.service.MedicamentoService;
import com.IntegraSaude.service.SessaoUsuario;
import com.IntegraSaude.service.ValidadorMedicamento;
import java.util.List;

/** Menu de medicamentos (somente para pacientes). */
public class MenuMedicamentos extends MenuBase {

    private final MedicamentoService medicamentos;
    private final SessaoUsuario sessao;

    public MenuMedicamentos(LeitorConsole leitor, SaidaConsole saida,
                            MedicamentoService medicamentos, SessaoUsuario sessao) {
        super(leitor, saida);
        this.medicamentos = medicamentos;
        this.sessao = sessao;
    }

    public void executar() {
        executarMenu(() -> "Medicamentos", "Voltar", () -> List.of(
                new Opcao("Cadastrar medicamento", this::cadastrar),
                new Opcao("Listar meus medicamentos", this::listar),
                new Opcao("Atualizar medicamento", this::atualizar),
                new Opcao("Remover medicamento", this::remover)));
    }

    private void cadastrar() {
        medicamentos.cadastrar(lerMedicamento());
        saida.exibir("Medicamento cadastrado.");
    }

    private void listar() {
        List<Medicamento> lista = medicamentos.listarDoPaciente(sessao.exigirLogado().getId());
        if (lista.isEmpty()) {
            saida.exibir("Nenhum medicamento cadastrado.");
        }
        for (Medicamento m : lista) {
            saida.exibir("[" + m.getId() + "] " + m.getNome() + " - " + m.getDosagem()
                    + " | a cada " + m.getFrequenciaHoras() + "h, a partir de "
                    + m.getPrimeiroHorario().format(Formatos.HORA)
                    + " | de " + m.getDataInicio().format(Formatos.DATA)
                    + " ate " + m.getDataFim().format(Formatos.DATA));
        }
    }

    // Atualiza refazendo as perguntas; o objeto novo leva o id do antigo
    private void atualizar() {
        listar();
        Long id = leitor.lerId("Codigo do medicamento a atualizar:");
        Medicamento novo = lerMedicamento();
        novo.setId(id);
        medicamentos.atualizar(novo);
        saida.exibir("Medicamento atualizado.");
    }

    private void remover() {
        listar();
        Long id = leitor.lerId("Codigo do medicamento a remover:");
        if (leitor.confirmar("Confirma a remocao?")) {
            medicamentos.remover(sessao.exigirLogado().getId(), id);
            saida.exibir("Medicamento removido.");
        }
    }

    // Lê os campos do medicamento (a validação de regra fica no serviço)
    private Medicamento lerMedicamento() {
        String nome = leitor.lerTexto("Nome:");
        String dosagem = leitor.lerTexto("Dosagem (ex.: 500 mg):");
        int frequencia = leitor.lerInteiro("Frequencia em horas ("
                + ValidadorMedicamento.FREQUENCIA_MINIMA_HORAS + " a "
                + ValidadorMedicamento.FREQUENCIA_MAXIMA_HORAS + "):", 0, Integer.MAX_VALUE);
        var primeiroHorario = leitor.lerHora("Primeiro horario do dia");
        var inicio = leitor.lerData("Data de inicio");
        var fim = leitor.lerData("Data de fim");
        return new Medicamento(sessao.exigirLogado().getId(), nome, dosagem, frequencia,
                primeiroHorario, inicio, fim);
    }
}
