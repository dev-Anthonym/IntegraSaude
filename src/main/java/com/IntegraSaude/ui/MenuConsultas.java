package com.IntegraSaude.ui;

import com.IntegraSaude.exception.AppException;
import com.IntegraSaude.model.Consulta;
import com.IntegraSaude.model.Endereco;
import com.IntegraSaude.model.Especialidade;
import com.IntegraSaude.model.Medico;
import com.IntegraSaude.service.ConsultaService;
import com.IntegraSaude.service.EnderecoService;
import com.IntegraSaude.service.EspecialidadeService;
import com.IntegraSaude.service.MedicoService;
import com.IntegraSaude.service.SessaoUsuario;
import java.time.LocalDateTime;
import java.util.List;

/** Menu de consultas (somente para pacientes). */
public class MenuConsultas extends MenuBase {

    private static final int CODIGO_NOVO = 0;

    private final ConsultaService consultas;
    private final EspecialidadeService especialidades;
    private final MedicoService medicos;
    private final EnderecoService enderecos;
    private final SessaoUsuario sessao;

    public MenuConsultas(LeitorConsole leitor, SaidaConsole saida, ConsultaService consultas,
                         EspecialidadeService especialidades, MedicoService medicos,
                         EnderecoService enderecos, SessaoUsuario sessao) {
        super(leitor, saida);
        this.consultas = consultas;
        this.especialidades = especialidades;
        this.medicos = medicos;
        this.enderecos = enderecos;
        this.sessao = sessao;
    }

    public void executar() {
        executarMenu(() -> "Consultas", "Voltar", () -> List.of(
                new Opcao("Cadastrar consulta", this::cadastrar),
                new Opcao("Listar minhas consultas", this::listar),
                new Opcao("Atualizar consulta", this::atualizar),
                new Opcao("Cancelar consulta", this::cancelar)));
    }

    private void cadastrar() {
        consultas.cadastrar(lerConsulta());
        saida.exibir("Consulta cadastrada.");
    }

    private void listar() {
        List<Consulta> lista = consultas.listarDoPaciente(sessao.exigirLogado().getId());
        if (lista.isEmpty()) {
            saida.exibir("Nenhuma consulta cadastrada.");
        }
        for (Consulta c : lista) {
            saida.exibir("[" + c.getId() + "] " + c.getDataHora().format(Formatos.DATA_HORA) + " - "
                    + c.getMedico().getEspecialidade().getNome() + " com " + c.getMedico().getNome()
                    + " em " + c.getLocal());
        }
    }

    private void atualizar() {
        listar();
        Long id = leitor.lerId("Codigo da consulta a atualizar:");
        Consulta nova = lerConsulta();
        nova.setId(id);
        consultas.atualizar(nova);
        saida.exibir("Consulta atualizada.");
    }

    private void cancelar() {
        listar();
        Long id = leitor.lerId("Codigo da consulta a cancelar:");
        if (leitor.confirmar("Confirma o cancelamento?")) {
            consultas.cancelar(sessao.exigirLogado().getId(), id);
            saida.exibir("Consulta cancelada.");
        }
    }

    // Lê médico, local, data e hora
    private Consulta lerConsulta() {
        Medico medico = escolherMedico(escolherEspecialidade());
        String local = lerLocal();
        var data = leitor.lerData("Data da consulta");
        var hora = leitor.lerHora("Hora da consulta");
        return new Consulta(sessao.exigirLogado().getId(), medico, local, LocalDateTime.of(data, hora));
    }

    // Lista as especialidades e deixa escolher uma ou cadastrar uma nova (código 0)
    private Especialidade escolherEspecialidade() {
        List<Especialidade> lista = especialidades.listar();
        lista.forEach(e -> saida.exibir("[" + e.getId() + "] " + e.getNome()));
        saida.exibir("[" + CODIGO_NOVO + "] Cadastrar nova especialidade");
        int codigo = leitor.lerInteiro("Especialidade:", CODIGO_NOVO, Integer.MAX_VALUE);
        if (codigo == CODIGO_NOVO) {
            return especialidades.cadastrar(leitor.lerTexto("Nome da nova especialidade:"));
        }
        return especialidades.buscarPorId((long) codigo);
    }

    // Lista os médicos da especialidade e deixa escolher um ou cadastrar um novo (código 0)
    private Medico escolherMedico(Especialidade especialidade) {
        medicos.listarPorEspecialidade(especialidade.getId())
                .forEach(m -> saida.exibir("[" + m.getId() + "] " + m.getNome()));
        saida.exibir("[" + CODIGO_NOVO + "] Cadastrar novo medico");
        int codigo = leitor.lerInteiro("Medico:", CODIGO_NOVO, Integer.MAX_VALUE);
        if (codigo == CODIGO_NOVO) {
            return medicos.cadastrar(leitor.lerTexto("Nome do medico:"), especialidade.getId());
        }
        return medicos.buscarPorId((long) codigo);
    }

    // Tenta preencher o endereço pelo CEP; se a API falhar, o usuário digita à mão
    private String lerLocal() {
        String cep = leitor.lerTextoOpcional("CEP do local (ENTER para digitar o endereco):");
        if (!cep.isEmpty()) {
            try {
                Endereco endereco = enderecos.buscarPorCep(cep);
                saida.exibir("Endereco encontrado: " + endereco.formatar());
                String complemento = leitor.lerTextoOpcional("Numero/complemento (opcional):");
                return complemento.isEmpty() ? endereco.formatar() : endereco.formatar() + " - " + complemento;
            } catch (AppException e) {
                saida.exibir("Nao foi possivel usar o CEP (" + e.getMessage() + "). Digite o endereco.");
            }
        }
        return leitor.lerTexto("Local da consulta:");
    }
}
