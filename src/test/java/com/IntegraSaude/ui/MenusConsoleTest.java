package com.IntegraSaude.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.CenarioTeste;
import com.IntegraSaude.NotificadorFalso;
import com.IntegraSaude.RelogioAjustavel;
import com.IntegraSaude.exception.EntradaEncerradaException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.model.Endereco;
import com.IntegraSaude.model.Perfil;
import com.IntegraSaude.model.Usuario;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import com.IntegraSaude.service.Servicos;
import com.IntegraSaude.service.SessaoUsuario;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Dirige os menus de console com uma entrada roteirizada (como um usuário digitando)
 * e confere o que foi mostrado na tela. "Agora" é 23/11/2026 10:00.
 */
class MenusConsoleTest {

    private final RelogioAjustavel relogio = new RelogioAjustavel(CenarioTeste.AGORA);
    private Servicos servicos;
    private DadosDemonstracao demonstracao;
    private SessaoUsuario sessao;

    @BeforeEach
    void preparar() {
        servicos = Servicos.montar(Repositorios.emMemoria(), ConfiguracaoLembretes.padrao(), relogio,
                new NotificadorFalso(), cep -> {
                    if (cep.equals("99999999")) {
                        throw new IllegalStateException("falha inesperada");
                    }
                    if (cep.equals("11111111")) {
                        throw new RegistroNaoEncontradoException("CEP nao encontrado");
                    }
                    return new Endereco(cep, "Rua A", "Centro", "Contagem", "MG");
                });
        demonstracao = new DadosDemonstracao(servicos.usuarios(), servicos.vinculos(), servicos.medicamentos(),
                servicos.especialidades(), servicos.medicos(), servicos.consultas(), relogio);
        sessao = new SessaoUsuario();
    }

    // Monta os menus como a Aplicacao faz, roda até "Sair" e devolve tudo o que foi impresso
    private String rodar(String... linhas) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SaidaConsole saida = new SaidaConsole(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        String entrada = String.join("\n", linhas) + "\n";
        LeitorConsole leitor = new LeitorConsole(new Scanner(entrada), saida);
        SelecaoPaciente selecao = new SelecaoPaciente(sessao, servicos.vinculos(), leitor, saida);
        MenuPrincipal menu = new MenuPrincipal(leitor, saida, sessao,
                new MenuUsuarios(leitor, saida, servicos.usuarios(), servicos.autenticacao(), servicos.vinculos(),
                        sessao),
                new MenuMedicamentos(leitor, saida, servicos.medicamentos(), sessao),
                new MenuConsultas(leitor, saida, servicos.consultas(), servicos.especialidades(), servicos.medicos(),
                        servicos.enderecos(), sessao),
                new MenuLembretes(leitor, saida, servicos.lembretes(), servicos.historico(), sessao, selecao),
                new MenuAgenda(leitor, saida, servicos.agenda(), selecao), demonstracao);
        menu.executar();
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void contem(String saida, String... trechos) {
        for (String trecho : trechos) {
            assertTrue(saida.contains(trecho), () -> "Faltou \"" + trecho + "\" na saida:\n" + saida);
        }
    }

    private static final String[] LOGIN_MARIA = {"2", DadosDemonstracao.EMAIL_PACIENTE, DadosDemonstracao.SENHA};
    private static final String[] LOGIN_JOAO = {"2", DadosDemonstracao.EMAIL_RESPONSAVEL, DadosDemonstracao.SENHA};

    private static String[] juntar(String[]... partes) {
        return java.util.Arrays.stream(partes).flatMap(java.util.Arrays::stream).toArray(String[]::new);
    }

    @Test
    void menuSemLoginDeveCadastrarRecusarErrosECarregarDemonstracao() {
        String saida = rodar(
                "9", "abc",
                "1", "", "Ana Paciente", "ana@teste.com", "Senha123", "3", "1",
                "1", "Ana", "ANA@teste.com", "Senha123", "2",
                "2", "ana@teste.com", "errada",
                "3", "3",
                "0");
        contem(saida,
                "=== Lembretes de Medicamentos e Consultas | sem login ===",
                "Digite um numero entre 0 e 3.",
                "Entrada invalida. Digite apenas numeros.",
                "Este campo e obrigatorio.",
                "Digite um numero entre 1 e 2.",
                "Usuario cadastrado com sucesso.",
                "Erro: Ja existe um usuario com este e-mail",
                "Erro: E-mail ou senha invalidos",
                "Dados carregados.",
                "Os dados de demonstracao ja foram carregados antes.");
        assertFalse(sessao.estaLogado());
    }

    @Test
    void pacienteDeveGerenciarMedicamentos() {
        demonstracao.carregar();
        String saida = rodar(juntar(LOGIN_MARIA, new String[] {
                "1",
                "2",
                "1", "Losartana", "50 mg", "30", "08:00", "23/11/2026", "30/11/2026",
                "1", "Losartana", "50 mg", "12", "25:00", "08:00", "xx", "23/11/2026", "30/11/2026",
                "3", "2", "Losartana", "100 mg", "24", "09:00", "23/11/2026", "30/11/2026",
                "4", "2", "talvez", "n",
                "4", "2", "s",
                "4", "99", "s",
                "0",
                "6", "0"}));
        contem(saida,
                "Bem-vindo(a), Maria Paciente!",
                "=== Lembretes de Medicamentos e Consultas | Maria Paciente (PACIENTE) ===",
                "[1] Dipirona - 500 mg | a cada 8h, a partir de 10:01 | de 23/11/2026 ate 30/11/2026",
                "Erro: A frequencia deve estar entre 1 e 24 horas",
                "Hora invalida. Use o formato HH:mm",
                "Data invalida. Use o formato dd/mm/aaaa",
                "Medicamento cadastrado.",
                "[2] Losartana - 50 mg | a cada 12h",
                "Medicamento atualizado.",
                "Responda com s ou n.",
                "Medicamento removido.",
                "Erro: Medicamento nao encontrado",
                "Sessao encerrada.");
        assertEquals(1, servicos.medicamentos().listarDoPaciente(sessaoMaria().getId()).size());
        assertFalse(sessao.estaLogado());
    }

    @Test
    void pacienteDeveGerenciarConsultasComCep() {
        demonstracao.carregar();
        String saida = rodar(juntar(LOGIN_MARIA, new String[] {
                "2",
                "1", "1", "1", "32010000", "120", "25/11/2026", "10:00",
                "1", "1", "1", "", "Clinica X", "25/11/2026", "10:00",
                "1", "0", "Dermatologia", "0", "Dra. Ana", "123", "Rua B", "26/11/2026", "14:00",
                "1", "1", "1", "11111111", "Rua C", "27/11/2026", "15:00",
                "1", "1", "1", "99999999",
                "2",
                "3", "2", "1", "1", "", "Clinica Nova", "28/11/2026", "08:00",
                "4", "2", "n",
                "4", "2", "s",
                "0", "0"}));
        contem(saida,
                "[1] Cardiologia",
                "[0] Cadastrar nova especialidade",
                "[1] Dr. Carlos Silva",
                "Endereco encontrado: Rua A, Centro - Contagem/MG",
                "Consulta cadastrada.",
                "Erro: O paciente ja tem uma consulta neste horario",
                "Nao foi possivel usar o CEP (O CEP deve ter 8 digitos). Digite o endereco.",
                "Nao foi possivel usar o CEP (CEP nao encontrado). Digite o endereco.",
                "Ocorreu um erro inesperado. Os detalhes foram registrados no log.",
                "[1] 24/11/2026 09:00 - Cardiologia com Dr. Carlos Silva em Clinica Saude, Belo Horizonte",
                "[2] 25/11/2026 10:00 - Cardiologia com Dr. Carlos Silva em Rua A, Centro - Contagem/MG - 120",
                "[3] 26/11/2026 14:00 - Dermatologia com Dra. Ana em Rua B",
                "Consulta atualizada.",
                "Consulta cancelada.");
        Long mariaId = sessaoMaria().getId();
        assertEquals(3, servicos.consultas().listarDoPaciente(mariaId).size(), "1, 3 e 4 continuam agendadas");
    }

    @Test
    void pacienteDeveConfirmarLembreteVerHistoricoAgendaEVinculos() {
        demonstracao.carregar();
        Long mariaId = sessaoMaria().getId();
        servicos.gerador().gerarDoDia(mariaId, CenarioTeste.AGORA.toLocalDate());
        String saida = rodar(juntar(LOGIN_MARIA, new String[] {
                "3",
                "1",
                "2", "1",
                "2", "99",
                "3",
                "4", "01/01/2020", "02/01/2020",
                "0",
                "4",
                "1", "24/11/2026",
                "1", "01/01/2020",
                "1", "ontem", "24/11/2026",
                "2", "24/11/2026",
                "0",
                "5",
                "1",
                "3", DadosDemonstracao.EMAIL_RESPONSAVEL,
                "1",
                "2", DadosDemonstracao.EMAIL_RESPONSAVEL,
                "2", DadosDemonstracao.EMAIL_RESPONSAVEL,
                "0",
                "6", "0"}));
        contem(saida,
                "[1] 23/11/2026 10:01 - Tomar Dipirona (500 mg)",
                "Confirmado! Registrado no historico.",
                "Erro: Lembrete nao encontrado",
                "| CONFIRMADO | Confirmado: Tomar Dipirona (500 mg)",
                "Nenhum evento no historico.",
                "24/11/2026 09:00 | CONSULTA | Consulta de Cardiologia com Dr. Carlos Silva",
                "Agenda vazia.",
                "Data invalida. Use o formato dd/mm/aaaa.",
                "30/11/2026 18:01 | MEDICAMENTO | Tomar Dipirona (500 mg)",
                "- Joao Responsavel <joao@demo.com>",
                "Vinculo removido.",
                "Nenhum vinculo cadastrado.",
                "Vinculo criado com Joao Responsavel.",
                "Erro: Este vinculo ja existe");
    }

    @Test
    void responsavelSoAcompanhaSemConfirmar() {
        demonstracao.carregar();
        servicos.gerador().gerarDoDia(sessaoMaria().getId(), CenarioTeste.AGORA.toLocalDate());
        String saida = rodar(juntar(LOGIN_JOAO, new String[] {
                "2",
                "1", "1",
                "2", "1",
                "0",
                "3", "1", "1", "24/11/2026", "0",
                "1", "1", "3", DadosDemonstracao.EMAIL_PACIENTE, "0",
                "2", "1", "0",
                "1", "2", DadosDemonstracao.EMAIL_PACIENTE, "0",
                "4", "0"}));
        contem(saida,
                "Joao Responsavel (RESPONSAVEL)",
                "1 - Meus pacientes",
                "Seus pacientes:",
                "1 - Maria Paciente",
                "[1] 23/11/2026 10:01 - Tomar Dipirona (500 mg)",
                "| GERADO | Gerado: Tomar Dipirona (500 mg)",
                "24/11/2026 09:00 | CONSULTA",
                "Listar meus pacientes",
                "- Maria Paciente <maria@demo.com>",
                "Vinculo removido.",
                "Voce ainda nao tem pacientes vinculados.",
                "Vinculo criado com Maria Paciente.");
        assertFalse(saida.contains("Confirmar dose/consulta"), "responsavel nao confirma");
        assertFalse(saida.contains("1 - Medicamentos"), "responsavel nao cadastra remedio");
    }

    @Test
    void fimDaEntradaDeveEncerrarOMenuDeFormaLimpa() {
        assertThrows(EntradaEncerradaException.class, () -> rodar("2", "maria@demo.com"));
        assertThrows(EntradaEncerradaException.class, () -> rodar("1", "Ana", "ana@teste.com"));
    }

    @Test
    void notificadorDeConsoleDeveDestacarOAviso() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        SaidaConsole saida = new SaidaConsole(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        Usuario joao = new Usuario("Joao", "joao@teste.com", "hash", Perfil.RESPONSAVEL);
        new NotificadorConsole(saida).notificar(joao, "ALERTA: teste");
        assertEquals(System.lineSeparator() + ">>> [para Joao] ALERTA: teste" + System.lineSeparator(),
                bytes.toString(StandardCharsets.UTF_8));
    }

    @Test
    void sessaoDoConsoleDeveExigirLogin() {
        SessaoUsuario nova = new SessaoUsuario();
        assertFalse(nova.estaLogado());
        assertTrue(nova.usuario().isEmpty());
        assertThrows(com.IntegraSaude.exception.OperacaoInvalidaException.class, nova::exigirLogado);
        Usuario ana = new Usuario("Ana", "ana@teste.com", "hash", Perfil.PACIENTE);
        nova.login(ana);
        assertEquals(ana, nova.exigirLogado());
        nova.logout();
        assertFalse(nova.estaLogado());
    }

    private Usuario sessaoMaria() {
        return servicos.usuarios().buscarPorEmail(DadosDemonstracao.EMAIL_PACIENTE);
    }
}
