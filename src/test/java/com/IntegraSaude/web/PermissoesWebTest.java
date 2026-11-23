package com.IntegraSaude.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.NotificadorFalso;
import com.IntegraSaude.RelogioAjustavel;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.exception.ServicoExternoException;
import com.IntegraSaude.model.Endereco;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import com.IntegraSaude.service.Servicos;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Login, sessão, rotas e permissões entre paciente e responsável, pelo HTTP de verdade. */
class PermissoesWebTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final LocalDate HOJE = AGORA.toLocalDate();

    private final RelogioAjustavel relogio = new RelogioAjustavel(AGORA);
    private Servicos servicos;
    private ServidorWeb servidor;

    // CEPs especiais simulam as falhas possíveis da API externa
    @BeforeEach
    void subirServidor() {
        servicos = Servicos.montar(Repositorios.emMemoria(), ConfiguracaoLembretes.padrao(), relogio,
                new NotificadorFalso(), cep -> switch (cep) {
                    case "00000000" -> throw new ServicoExternoException("fora do ar");
                    case "11111111" -> throw new RegistroNaoEncontradoException("CEP nao encontrado");
                    case "99999999" -> throw new IllegalStateException("falha inesperada");
                    default -> new Endereco(cep, "Rua A", "Centro", "Contagem", "MG");
                });
        servidor = new ServidorWeb(servicos, 0);
        servidor.iniciar();
    }

    @AfterEach
    void pararServidor() {
        servidor.parar();
    }

    private NavegadorTeste navegador() {
        return new NavegadorTeste(servidor);
    }

    private NavegadorTeste paciente(String nome, String email) throws Exception {
        return navegador().cadastrar(nome, email, "PACIENTE");
    }

    private NavegadorTeste responsavel(String nome, String email) throws Exception {
        return navegador().cadastrar(nome, email, "RESPONSAVEL");
    }

    // ---------------------------------------------------------------- Permissões por rota

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"/api/paciente/agenda", "/api/paciente/semana", "/api/paciente/lembretes",
            "/api/paciente/historico", "/api/paciente/medicamentos", "/api/paciente/consultas",
            "/api/paciente/responsaveis", "/api/especialidades", "/api/cep?cep=32010000"})
    void rotasGetDoPacienteDevemExigirLoginEPerfil(String rota) throws Exception {
        assertEquals(401, navegador().get(rota).statusCode());
        assertEquals(403, responsavel("Joao", "joao@teste.com").get(rota).statusCode());
        assertEquals(200, paciente("Maria", "maria@teste.com").get(rota).statusCode());
    }

    @ParameterizedTest(name = "POST {0}")
    @ValueSource(strings = {"/api/paciente/lembretes/confirmar", "/api/paciente/medicamentos",
            "/api/paciente/medicamentos/remover", "/api/paciente/consultas", "/api/paciente/consultas/cancelar",
            "/api/paciente/responsaveis", "/api/paciente/responsaveis/remover"})
    void rotasPostDoPacienteDevemExigirLoginEPerfil(String rota) throws Exception {
        assertEquals(401, navegador().post(rota, "id", "1").statusCode());
        assertEquals(403, responsavel("Joao", "joao@teste.com").post(rota, "id", "1").statusCode());
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(strings = {"/api/responsavel/pacientes", "/api/responsavel/lembretes",
            "/api/responsavel/historico", "/api/responsavel/agenda", "/api/responsavel/semana"})
    void rotasDoResponsavelDevemExigirLoginEPerfil(String rota) throws Exception {
        assertEquals(401, navegador().get(rota).statusCode());
        HttpResponse<String> doPaciente = paciente("Maria", "maria@teste.com").get(rota);
        assertEquals(403, doPaciente.statusCode());
        assertTrue(doPaciente.body().contains("Disponivel apenas para responsaveis"));
        assertEquals(200, responsavel("Joao", "joao@teste.com").get(rota).statusCode());
    }

    @Test
    void rotaInexistenteOuMetodoErradoDevemDar404() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        assertEquals(404, maria.get("/api/nao-existe").statusCode());
        assertEquals(404, maria.get("/api/login").statusCode(), "login so aceita POST");
        assertEquals(404, maria.post("/api/sessao").statusCode(), "sessao so aceita GET");
        HttpResponse<String> put = maria.enviar(HttpRequest.newBuilder(maria.uri("/api/sessao"))
                .PUT(HttpRequest.BodyPublishers.noBody()));
        assertEquals(404, put.statusCode());
    }

    @Test
    void arquivosEstaticosSoAceitamGetEHead() throws Exception {
        NavegadorTeste anonimo = navegador();
        HttpResponse<String> head = anonimo.enviar(HttpRequest.newBuilder(anonimo.uri("/estilo.css"))
                .method("HEAD", HttpRequest.BodyPublishers.noBody()));
        assertEquals(200, head.statusCode());
        assertTrue(head.body().isEmpty());
        assertEquals(405, anonimo.post("/index.html").statusCode());
        assertEquals(200, anonimo.get("/icone.svg").statusCode());
        assertEquals("nosniff", anonimo.get("/").headers().firstValue("X-Content-Type-Options").orElse(""));
    }

    @Test
    void corpoMaiorQue64KbDeveSerRecusado() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        HttpResponse<String> resposta = maria.post("/api/paciente/medicamentos", "nome", "x".repeat(70_000));
        assertEquals(400, resposta.statusCode());
        assertTrue(resposta.body().contains("Requisicao muito grande"));
    }

    // ---------------------------------------------------------------- Login e sessão

    @Test
    void cookieDeSessaoDeveSerHttpOnlyESameSiteStrict() throws Exception {
        NavegadorTeste maria = navegador();
        HttpResponse<String> resposta = maria.post("/api/cadastro", "nome", "Maria", "email", "maria@teste.com",
                "senha", NavegadorTeste.SENHA, "perfil", "paciente");
        String cookie = resposta.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.startsWith(Requisicao.COOKIE_SESSAO + "="));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Max-Age=" + SessoesWeb.DURACAO.toSeconds()));
        assertTrue(resposta.body().contains("\"perfil\":\"PACIENTE\""), "perfil aceita minusculas");
    }

    @Test
    void loginDeveIgnorarMaiusculasEEspacosNoEmail() throws Exception {
        paciente("Maria Souza", "maria@teste.com");
        HttpResponse<String> resposta = navegador().post("/api/login", "email", "  MARIA@Teste.com ",
                "senha", NavegadorTeste.SENHA);
        assertEquals(200, resposta.statusCode(), resposta.body());
        assertTrue(resposta.body().contains("\"primeiroNome\":\"Maria\""));
    }

    @Test
    void loginComEmailInexistenteDeveTerAMesmaMensagemDaSenhaErrada() throws Exception {
        paciente("Maria", "maria@teste.com");
        HttpResponse<String> semConta = navegador().post("/api/login", "email", "ninguem@teste.com",
                "senha", NavegadorTeste.SENHA);
        HttpResponse<String> senhaErrada = navegador().post("/api/login", "email", "maria@teste.com",
                "senha", "Errada999");
        assertEquals(401, semConta.statusCode());
        assertEquals(semConta.body(), senhaErrada.body());
        assertTrue(semConta.headers().firstValue("Set-Cookie").isEmpty(), "falha nao abre sessao");
    }

    @Test
    void novoLoginDeveTrocarOTokenEInvalidarOAntigo() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        String tokenAntigo = maria.cookie;
        assertEquals(200, maria.post("/api/login", "email", "maria@teste.com", "senha", NavegadorTeste.SENHA)
                .statusCode());
        assertNotEquals(tokenAntigo, maria.cookie);

        NavegadorTeste roubado = navegador();
        roubado.cookie = tokenAntigo;
        assertEquals(401, roubado.get("/api/sessao").statusCode());
        assertEquals(200, maria.get("/api/sessao").statusCode());
    }

    @Test
    void logoutDeveInvalidarOTokenNoServidor() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        String token = maria.cookie;
        HttpResponse<String> logout = maria.post("/api/logout");
        assertEquals(200, logout.statusCode());
        assertTrue(logout.headers().firstValue("Set-Cookie").orElseThrow().contains("Max-Age=0"));

        NavegadorTeste reaproveitado = navegador();
        reaproveitado.cookie = token;
        assertEquals(401, reaproveitado.get("/api/sessao").statusCode());
        assertEquals(200, navegador().post("/api/logout").statusCode(), "logout sem sessao nao falha");
    }

    @Test
    void sessaoDeveExpirarDepoisDe12Horas() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        relogio.avancar(SessoesWeb.DURACAO);
        assertEquals(200, maria.get("/api/sessao").statusCode(), "exatamente no limite ainda vale");
        relogio.avancar(Duration.ofSeconds(1));
        assertEquals(401, maria.get("/api/sessao").statusCode());
    }

    @Test
    void cookieInventadoOuVazioNaoDaAcesso() throws Exception {
        paciente("Maria", "maria@teste.com");
        NavegadorTeste invasor = navegador();
        invasor.cookie = Requisicao.COOKIE_SESSAO + "=token-inventado";
        assertEquals(401, invasor.get("/api/sessao").statusCode());
        invasor.cookie = "outro=1; " + Requisicao.COOKIE_SESSAO + "=";
        assertEquals(401, invasor.get("/api/sessao").statusCode());
    }

    @Test
    void cadastroDeveDevolverErrosComOCodigoCerto() throws Exception {
        paciente("Maria", "maria@teste.com");
        HttpResponse<String> perfilInvalido = navegador().post("/api/cadastro", "nome", "X", "email", "x@teste.com",
                "senha", NavegadorTeste.SENHA, "perfil", "ADMIN");
        assertEquals(400, perfilInvalido.statusCode());
        assertTrue(perfilInvalido.body().contains("O perfil e obrigatorio"));

        HttpResponse<String> duplicado = navegador().post("/api/cadastro", "nome", "Outra", "email",
                "MARIA@teste.com", "senha", NavegadorTeste.SENHA, "perfil", "PACIENTE");
        assertEquals(409, duplicado.statusCode());

        HttpResponse<String> senhaFraca = navegador().post("/api/cadastro", "nome", "Y", "email", "y@teste.com",
                "senha", "curta1", "perfil", "PACIENTE");
        assertEquals(400, senhaFraca.statusCode());
        assertTrue(senhaFraca.headers().firstValue("Set-Cookie").isEmpty());
    }

    @Test
    void parametroMalFormadoDeveDar400() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        HttpResponse<String> malFormado = maria.enviar(HttpRequest.newBuilder(maria.uri("/api/paciente/consultas/cancelar"))
                .header("X-Requested-With", "fetch")
                .POST(HttpRequest.BodyPublishers.ofString("id=%ZZ")));
        assertEquals(400, malFormado.statusCode());
        assertTrue(malFormado.body().contains("Parametro mal formado"));
        assertEquals(400, maria.get("/api/paciente/agenda?dia=23/11/2026").statusCode());
        assertEquals(400, maria.post("/api/paciente/consultas/cancelar", "id", "abc").statusCode());
    }

    // ---------------------------------------------------------------- Isolamento entre pacientes

    @Test
    void pacienteNaoPodeAlterarDadosDeOutroPaciente() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        NavegadorTeste pedro = paciente("Pedro", "pedro@teste.com");
        String amanha = HOJE.plusDays(1).toString();
        maria.post("/api/paciente/medicamentos", "nome", "Losartana", "dosagem", "50 mg", "frequencia", "24",
                "primeiroHorario", "10:00", "inicio", HOJE.toString(), "fim", amanha);
        maria.post("/api/paciente/consultas", "especialidade", "novo", "novaEspecialidade", "Cardiologia",
                "novoMedico", "Dr. Silva", "data", amanha, "hora", "09:00", "local", "Clinica");
        Long mariaId = servicos.usuarios().buscarPorEmail("maria@teste.com").getId();
        Long lembreteId = servicos.gerador().gerarDoDia(mariaId, HOJE).getFirst().getId();

        assertEquals(404, pedro.post("/api/paciente/medicamentos/remover", "id", "1").statusCode());
        assertEquals(404, pedro.post("/api/paciente/consultas/cancelar", "id", "1").statusCode());
        assertEquals(404, pedro.post("/api/paciente/lembretes/confirmar", "id", lembreteId.toString()).statusCode());
        assertFalse(pedro.get("/api/paciente/medicamentos").body().contains("Losartana"));
        assertFalse(pedro.get("/api/paciente/consultas").body().contains("Cardiologia"));
        assertEquals("[]", pedro.get("/api/paciente/medicamentos").body());

        // Os dados da Maria continuam intactos
        assertTrue(maria.get("/api/paciente/medicamentos").body().contains("Losartana"));
        assertTrue(maria.get("/api/paciente/lembretes").body().contains("\"status\":\"PENDENTE\""));
    }

    @Test
    void pacienteNaoPodeVincularOutroPacienteNemASiMesmo() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        paciente("Pedro", "pedro@teste.com");
        assertEquals(400, maria.post("/api/paciente/responsaveis", "email", "maria@teste.com").statusCode());
        assertEquals(400, maria.post("/api/paciente/responsaveis", "email", "pedro@teste.com").statusCode());
        assertEquals(404, maria.post("/api/paciente/responsaveis", "email", "ninguem@teste.com").statusCode());
        assertEquals(404, maria.post("/api/paciente/responsaveis/remover", "email", "pedro@teste.com").statusCode());
        assertEquals("[]", maria.get("/api/paciente/responsaveis").body());
    }

    @Test
    void responsavelSoEnxergaPacientesVinculados() throws Exception {
        NavegadorTeste maria = paciente("Maria Souza", "maria@teste.com");
        NavegadorTeste pedro = paciente("Pedro Lima", "pedro@teste.com");
        NavegadorTeste joao = responsavel("Joao", "joao@teste.com");
        NavegadorTeste ana = responsavel("Ana", "ana@teste.com");
        maria.post("/api/paciente/responsaveis", "email", "joao@teste.com");
        pedro.post("/api/paciente/responsaveis", "email", "ana@teste.com");
        String amanha = HOJE.plusDays(1).toString();
        maria.post("/api/paciente/medicamentos", "nome", "Losartana", "dosagem", "50 mg", "frequencia", "24",
                "primeiroHorario", "20:00", "inicio", HOJE.toString(), "fim", amanha);
        pedro.post("/api/paciente/medicamentos", "nome", "Insulina", "dosagem", "10 UI", "frequencia", "24",
                "primeiroHorario", "20:00", "inicio", HOJE.toString(), "fim", amanha);

        for (String rota : new String[] {"/api/responsavel/pacientes", "/api/responsavel/lembretes",
                "/api/responsavel/agenda", "/api/responsavel/semana"}) {
            String doJoao = joao.get(rota).body();
            assertTrue(doJoao.contains("Maria"), rota + ": " + doJoao);
            assertFalse(doJoao.contains("Pedro"), rota);
            assertFalse(doJoao.contains("Insulina"), rota);
            assertFalse(ana.get(rota).body().contains("Maria"), rota);
        }
    }

    // ---------------------------------------------------------------- Dados do responsável

    @Test
    void responsavelDeveAcompanharLembretesHistoricoEAgenda() throws Exception {
        NavegadorTeste maria = paciente("Maria Souza", "maria@teste.com");
        NavegadorTeste joao = responsavel("Joao", "joao@teste.com");
        maria.post("/api/paciente/responsaveis", "email", "joao@teste.com");
        String fim = HOJE.plusDays(7).toString();
        maria.post("/api/paciente/medicamentos", "nome", "Losartana", "dosagem", "50 mg", "frequencia", "24",
                "primeiroHorario", "10:00", "inicio", HOJE.toString(), "fim", fim);
        maria.post("/api/paciente/medicamentos", "nome", "Vitamina D", "dosagem", "1 cp", "frequencia", "24",
                "primeiroHorario", "20:00", "inicio", HOJE.toString(), "fim", fim);
        String amanha = HOJE.plusDays(1).toString();
        maria.post("/api/paciente/consultas", "especialidade", "novo", "novaEspecialidade", "Cardiologia",
                "novoMedico", "Dr. Silva", "data", amanha, "hora", "09:00", "local", "Clinica");

        // Gera o lembrete das 10:00 e deixa ele virar perdido com alerta às 10:31
        Long mariaId = servicos.usuarios().buscarPorEmail("maria@teste.com").getId();
        servicos.gerador().gerarDoDia(mariaId, HOJE);
        relogio.avancar(Duration.ofMinutes(31));
        servicos.lembretes().verificarPerdidos(LocalDateTime.now(relogio))
                .forEach(servicos.alertas()::alertarResponsaveis);

        String lembretes = joao.get("/api/responsavel/lembretes").body();
        int perdido = lembretes.indexOf("\"status\":\"PERDIDO\"");
        int emBreve = lembretes.indexOf("\"status\":\"EM_BREVE\"");
        assertTrue(perdido >= 0 && emBreve > perdido, "perdidos vem antes dos proximos: " + lembretes);
        assertTrue(lembretes.contains("\"titulo\":\"Vitamina D 1 cp\""));
        assertTrue(lembretes.contains("\"prazo\":\"10:30\""));

        String historico = joao.get("/api/responsavel/historico").body();
        assertTrue(historico.contains("\"alertaAs\":\"10:31\""), historico);
        assertTrue(historico.contains("\"primeiroNome\":\"Maria\""));

        String agendaAmanha = joao.get("/api/responsavel/agenda?dia=" + amanha).body();
        assertTrue(agendaAmanha.contains("\"status\":\"AGENDADA\""), agendaAmanha);
        assertTrue(agendaAmanha.contains("\"subtitulo\":\"Consulta · Dr. Silva\""));
        assertEquals(400, joao.get("/api/responsavel/agenda?dia=amanha").statusCode());

        String semana = joao.get("/api/responsavel/semana?inicio=" + HOJE).body();
        assertTrue(semana.contains("{\"data\":\"" + amanha + "\",\"linhas\":[{\"paciente\":\"Maria\","
                + "\"doses\":2,\"consultas\":1}]}"), semana);

        // Responsável só acompanha: não confirma
        assertEquals(403, joao.post("/api/paciente/lembretes/confirmar", "id", "1").statusCode());
    }

    @Test
    void responsavelSemPacientesRecebeListasVazias() throws Exception {
        NavegadorTeste joao = responsavel("Joao", "joao@teste.com");
        assertEquals("{\"tempoLimite\":30,\"alerta\":null,\"pacientes\":[]}",
                joao.get("/api/responsavel/pacientes").body());
        assertEquals("{\"itens\":[]}", joao.get("/api/responsavel/lembretes").body());
    }

    // ---------------------------------------------------------------- Prazo pela web

    @Test
    void confirmacaoPelaWebDeveRespeitarOPrazoDe30Minutos() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        String fim = HOJE.plusDays(1).toString();
        maria.post("/api/paciente/medicamentos", "nome", "A", "dosagem", "1", "frequencia", "24",
                "primeiroHorario", "10:00", "inicio", HOJE.toString(), "fim", fim);
        maria.post("/api/paciente/medicamentos", "nome", "B", "dosagem", "1", "frequencia", "24",
                "primeiroHorario", "10:00", "inicio", HOJE.toString(), "fim", fim);
        Long mariaId = servicos.usuarios().buscarPorEmail("maria@teste.com").getId();
        servicos.gerador().gerarDoDia(mariaId, HOJE);

        relogio.avancar(Duration.ofMinutes(30));
        assertEquals(200, maria.post("/api/paciente/lembretes/confirmar", "id", "1").statusCode());
        relogio.avancar(Duration.ofSeconds(1));
        HttpResponse<String> atrasada = maria.post("/api/paciente/lembretes/confirmar", "id", "2");
        assertEquals(400, atrasada.statusCode());
        assertTrue(atrasada.body().contains("prazo"));
        assertEquals(400, maria.post("/api/paciente/lembretes/confirmar", "id", "1").statusCode(),
                "ja confirmado");
        assertEquals(404, maria.post("/api/paciente/lembretes/confirmar", "id", "99").statusCode());
    }

    // ---------------------------------------------------------------- Consultas e CEP

    @Test
    void cadastroDeConsultaDeveValidarEspecialidadeEMedico() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        String amanha = HOJE.plusDays(1).toString();
        maria.post("/api/paciente/consultas", "especialidade", "novo", "novaEspecialidade", "Cardiologia",
                "novoMedico", "Dr. Silva", "data", amanha, "hora", "09:00", "local", "Clinica");
        maria.post("/api/paciente/consultas", "especialidade", "novo", "novaEspecialidade", "Dermatologia",
                "novoMedico", "Dra. Ana", "data", amanha, "hora", "10:00", "local", "Clinica");

        assertEquals(400, maria.post("/api/paciente/consultas", "data", amanha, "hora", "11:00",
                "local", "X").statusCode(), "sem especialidade");
        assertEquals(400, maria.post("/api/paciente/consultas", "especialidade", "1", "data", amanha,
                "hora", "11:00", "local", "X").statusCode(), "sem medico");
        HttpResponse<String> trocado = maria.post("/api/paciente/consultas", "especialidade", "1", "medico", "2",
                "data", amanha, "hora", "11:00", "local", "X");
        assertEquals(400, trocado.statusCode());
        assertTrue(trocado.body().contains("nao e desta especialidade"));
        assertEquals(404, maria.post("/api/paciente/consultas", "especialidade", "9", "medico", "1",
                "data", amanha, "hora", "11:00", "local", "X").statusCode());
        assertEquals(200, maria.post("/api/paciente/consultas", "especialidade", "1", "medico", "novo",
                "novoMedico", "Dr. Novo", "data", amanha, "hora", "11:00", "local", "X").statusCode());
        assertEquals(400, maria.post("/api/paciente/consultas", "especialidade", "1", "medico", "1",
                "data", HOJE.minusDays(1).toString(), "hora", "11:00", "local", "X").statusCode(), "passado");

        String especialidades = maria.get("/api/especialidades").body();
        assertTrue(especialidades.contains("{\"id\":3,\"nome\":\"Dr. Novo\"}"), especialidades);
    }

    @Test
    void consultasDevemVirFuturasPrimeiroEPassadasSemCancelamento() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        maria.post("/api/paciente/consultas", "especialidade", "novo", "novaEspecialidade", "Cardiologia",
                "novoMedico", "Dr. Silva", "data", HOJE.toString(), "hora", "10:30", "local", "Clinica");
        maria.post("/api/paciente/consultas", "especialidade", "1", "medico", "1",
                "data", HOJE.plusDays(2).toString(), "hora", "08:00", "local", "Clinica");
        relogio.avancar(Duration.ofHours(1));

        String consultas = maria.get("/api/paciente/consultas").body();
        int futura = consultas.indexOf("\"hora\":\"08:00\"");
        int passada = consultas.indexOf("\"hora\":\"10:30\"");
        assertTrue(futura >= 0 && passada > futura, consultas);
        assertTrue(consultas.contains("\"podeCancelar\":false"));
        assertEquals(400, maria.post("/api/paciente/consultas/cancelar", "id", "1").statusCode());
    }

    @Test
    void falhasDoCepDevemVirarCodigosHttpDistintos() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        assertEquals(502, maria.get("/api/cep?cep=00000-000").statusCode());
        assertEquals(404, maria.get("/api/cep?cep=11111111").statusCode());
        HttpResponse<String> inesperado = maria.get("/api/cep?cep=99999999");
        assertEquals(500, inesperado.statusCode());
        assertTrue(inesperado.body().contains("Erro inesperado"), "nao vaza detalhes internos");
        assertFalse(inesperado.body().contains("falha inesperada"));
    }

    // ---------------------------------------------------------------- Remédios pela web

    @Test
    void remediosDevemMostrarHorariosEncerramentoERemocao() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        maria.post("/api/paciente/medicamentos", "nome", "Amoxicilina", "dosagem", "500 mg", "frequencia", "8",
                "primeiroHorario", "22:00", "inicio", HOJE.toString(), "fim", HOJE.plusDays(2).toString());
        assertEquals(400, maria.post("/api/paciente/medicamentos", "nome", "X", "dosagem", "1",
                "frequencia", "oito", "primeiroHorario", "08:00", "inicio", HOJE.toString(),
                "fim", HOJE.toString()).statusCode());
        assertEquals(400, maria.post("/api/paciente/medicamentos", "nome", "X", "dosagem", "1",
                "frequencia", "8", "primeiroHorario", "8h", "inicio", HOJE.toString(),
                "fim", HOJE.toString()).statusCode());

        // No primeiro dia a referencia e o dia seguinte: 06:00, 14:00 e 22:00
        String remedios = maria.get("/api/paciente/medicamentos").body();
        assertTrue(remedios.contains("\"horarios\":[\"06:00\",\"14:00\",\"22:00\"]"), remedios);
        assertTrue(remedios.contains("\"encerrado\":false"));

        relogio.avancar(Duration.ofDays(3));
        assertEquals(401, maria.get("/api/paciente/medicamentos").statusCode(), "sessao de 12h expirou");
        maria.post("/api/login", "email", "maria@teste.com", "senha", NavegadorTeste.SENHA);
        String depois = maria.get("/api/paciente/medicamentos").body();
        assertTrue(depois.contains("\"encerrado\":true"), depois);

        assertEquals(200, maria.post("/api/paciente/medicamentos/remover", "id", "1").statusCode());
        assertEquals("[]", maria.get("/api/paciente/medicamentos").body());
        assertEquals(404, maria.post("/api/paciente/medicamentos/remover", "id", "1").statusCode());
    }

    @Test
    void agendaDoPacienteDeveMostrarStatusDeCadaDose() throws Exception {
        NavegadorTeste maria = paciente("Maria", "maria@teste.com");
        maria.post("/api/paciente/medicamentos", "nome", "Losartana", "dosagem", "50 mg", "frequencia", "6",
                "primeiroHorario", "04:00", "inicio", HOJE.toString(), "fim", HOJE.plusDays(1).toString());
        Long mariaId = servicos.usuarios().buscarPorEmail("maria@teste.com").getId();
        relogio.avancar(Duration.ofMinutes(-60 * 6 + 10));  // 04:10: gera a dose das 04:00
        servicos.gerador().gerarDoDia(mariaId, HOJE);
        relogio.avancar(Duration.ofMinutes(60 * 6 - 10));   // de volta às 10:00
        servicos.gerador().gerarDoDia(mariaId, HOJE);
        servicos.lembretes().verificarPerdidos(LocalDateTime.now(relogio));
        maria.post("/api/paciente/lembretes/confirmar", "id", "2");

        String agenda = maria.get("/api/paciente/agenda").body();
        assertTrue(agenda.contains("{\"hora\":\"04:00\",\"tipo\":\"MEDICAMENTO\",\"titulo\":\"Losartana 50 mg\","
                + "\"nome\":\"Losartana\",\"subtitulo\":\"Medicamento\",\"status\":\"PERDIDO\""), agenda);
        assertTrue(agenda.contains("\"hora\":\"10:00\",\"tipo\":\"MEDICAMENTO\",\"titulo\":\"Losartana 50 mg\","
                + "\"nome\":\"Losartana\",\"subtitulo\":\"Medicamento\",\"status\":\"CONFIRMADO\""));
        assertTrue(agenda.contains("\"agora\":null"));
        String ontem = maria.get("/api/paciente/agenda?dia=" + HOJE.minusDays(1)).body();
        assertTrue(ontem.contains("\"itens\":[]"));

        String semana = maria.get("/api/paciente/semana").body();
        assertTrue(semana.contains("\"horariosRemedios\":[\"04:00\",\"10:00\",\"16:00\",\"22:00\"]"), semana);
    }
}
