package com.IntegraSaude.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.IntegraSaude.NotificadorFalso;
import com.IntegraSaude.RelogioAjustavel;
import com.IntegraSaude.model.Endereco;
import com.IntegraSaude.model.Lembrete;
import com.IntegraSaude.repository.Repositorios;
import com.IntegraSaude.service.ConfiguracaoLembretes;
import com.IntegraSaude.service.Servicos;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Sobe o servidor web de verdade (porta livre, repositórios em memória, relógio controlado)
 * e faz requisições HTTP como o navegador faria.
 */
class ServidorWebTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 23, 10, 0);
    private static final String SENHA = "Senha123";

    private final RelogioAjustavel relogio = new RelogioAjustavel(AGORA);
    private Servicos servicos;
    private ServidorWeb servidor;

    // Cada "navegador" guarda o próprio cookie de sessão. O CookieManager do JDK recusa cookies
    // de hosts sem ponto (como "localhost"), por isso o cookie é guardado aqui, como o navegador faz
    private final class Navegador {
        private final HttpClient cliente = HttpClient.newHttpClient();
        private String cookie = "";

        HttpResponse<String> get(String caminho) throws IOException, InterruptedException {
            return enviar(HttpRequest.newBuilder(uri(caminho)).GET());
        }

        HttpResponse<String> post(String caminho, String... chavesEValores) throws IOException, InterruptedException {
            return enviar(HttpRequest.newBuilder(uri(caminho))
                    .header("X-Requested-With", "fetch")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formulario(chavesEValores))));
        }

        private HttpResponse<String> enviar(HttpRequest.Builder requisicao) throws IOException, InterruptedException {
            if (!cookie.isEmpty()) {
                requisicao.header("Cookie", cookie);
            }
            HttpResponse<String> resposta = cliente.send(requisicao.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            resposta.headers().firstValue("Set-Cookie").ifPresent(valor -> cookie = valor.split(";", 2)[0]);
            return resposta;
        }

        HttpResponse<String> cadastrar(String nome, String email, String perfil) throws Exception {
            HttpResponse<String> resposta = post("/api/cadastro", "nome", nome, "email", email, "senha", SENHA,
                    "perfil", perfil);
            assertEquals(200, resposta.statusCode(), resposta.body());
            return resposta;
        }
    }

    @BeforeEach
    void subirServidor() {
        servicos = Servicos.montar(Repositorios.emMemoria(), ConfiguracaoLembretes.padrao(), relogio,
                new NotificadorFalso(),
                cep -> new Endereco(cep, "Rua das Acácias", "Centro", "Contagem", "MG"));
        servidor = new ServidorWeb(servicos, 0);
        servidor.iniciar();
    }

    @AfterEach
    void pararServidor() {
        servidor.parar();
    }

    private URI uri(String caminho) {
        return URI.create("http://localhost:" + servidor.porta() + caminho);
    }

    private static String formulario(String... chavesEValores) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (int i = 0; i < chavesEValores.length; i += 2) {
            campos.put(chavesEValores[i], chavesEValores[i + 1]);
        }
        return campos.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    @Test
    void deveServirAPaginaEOsArquivosConhecidos() throws Exception {
        Navegador navegador = new Navegador();
        HttpResponse<String> pagina = navegador.get("/");
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("app.js"));
        assertTrue(pagina.headers().firstValue("Content-Security-Policy").isPresent());
        assertEquals(200, navegador.get("/app.js").statusCode());
        assertEquals(200, navegador.get("/estilo.css").statusCode());
        assertEquals(404, navegador.get("/config.properties").statusCode());
        assertEquals(404, navegador.get("/web/../config.properties").statusCode());
    }

    @Test
    void deveExigirLoginEPerfilCorreto() throws Exception {
        Navegador anonimo = new Navegador();
        assertEquals(401, anonimo.get("/api/paciente/agenda").statusCode());

        Navegador responsavel = new Navegador();
        responsavel.cadastrar("Joao Souza", "joao@teste.com", "RESPONSAVEL");
        assertEquals(403, responsavel.get("/api/paciente/agenda").statusCode());
        assertEquals(200, responsavel.get("/api/responsavel/pacientes").statusCode());

        responsavel.post("/api/logout");
        assertEquals(401, responsavel.get("/api/responsavel/pacientes").statusCode());
    }

    @Test
    void deveRecusarPostSemCabecalhoDoFrontend() throws Exception {
        HttpClient cliente = HttpClient.newHttpClient();
        HttpResponse<String> resposta = cliente.send(HttpRequest.newBuilder(uri("/api/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString("email=a@b.com&senha=x")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(403, resposta.statusCode());
    }

    @Test
    void deveResponderLoginInvalidoComMensagemGenerica() throws Exception {
        Navegador navegador = new Navegador();
        navegador.cadastrar("Maria Souza", "maria@teste.com", "PACIENTE");
        HttpResponse<String> resposta = new Navegador().post("/api/login", "email", "maria@teste.com",
                "senha", "errada123");
        assertEquals(401, resposta.statusCode());
        assertTrue(resposta.body().contains("E-mail ou senha invalidos"));
    }

    @Test
    void pacienteDeveCadastrarRemedioEVerNaAgenda() throws Exception {
        Navegador paciente = new Navegador();
        paciente.cadastrar("José Antônio", "jose@teste.com", "PACIENTE");
        LocalDate hoje = AGORA.toLocalDate();

        HttpResponse<String> invalido = paciente.post("/api/paciente/medicamentos", "nome", "Losartana",
                "dosagem", "50 mg", "frequencia", "30", "primeiroHorario", "08:00",
                "inicio", hoje.toString(), "fim", hoje.plusDays(7).toString());
        assertEquals(400, invalido.statusCode());

        HttpResponse<String> criado = paciente.post("/api/paciente/medicamentos", "nome", "Losartana",
                "dosagem", "50 mg", "frequencia", "12", "primeiroHorario", "08:00",
                "inicio", hoje.toString(), "fim", hoje.plusDays(7).toString());
        assertEquals(200, criado.statusCode(), criado.body());

        String agenda = paciente.get("/api/paciente/agenda").body();
        assertTrue(agenda.contains("\"titulo\":\"Losartana 50 mg\""), agenda);
        assertTrue(agenda.contains("\"hora\":\"08:00\""));
        assertTrue(agenda.contains("\"status\":\"EM_BREVE\""), "20:00 ainda vai acontecer");

        String remedios = paciente.get("/api/paciente/medicamentos").body();
        assertTrue(remedios.contains("\"horarios\":[\"08:00\",\"20:00\"]"), remedios);

        String sessao = paciente.get("/api/sessao").body();
        assertTrue(sessao.contains("José Antônio"), "acentos devem ir e voltar em UTF-8");
    }

    @Test
    void deveAgendarConsultaComNovaEspecialidadeECep() throws Exception {
        Navegador paciente = new Navegador();
        paciente.cadastrar("Maria Souza", "maria@teste.com", "PACIENTE");

        String endereco = paciente.get("/api/cep?cep=32010-000").body();
        assertTrue(endereco.contains("\"cidade\":\"Contagem\""), endereco);
        assertEquals(400, paciente.get("/api/cep?cep=123").statusCode());

        String amanha = AGORA.toLocalDate().plusDays(1).toString();
        HttpResponse<String> criada = paciente.post("/api/paciente/consultas", "especialidade", "novo",
                "novaEspecialidade", "Clínico geral", "novoMedico", "Dra. Ana Reis", "data", amanha,
                "hora", "09:00", "local", "Rua das Acácias, 120");
        assertEquals(200, criada.statusCode(), criada.body());

        HttpResponse<String> conflito = paciente.post("/api/paciente/consultas", "especialidade", "1",
                "medico", "1", "data", amanha, "hora", "09:00", "local", "Outro lugar");
        assertEquals(409, conflito.statusCode(), "mesmo horario para o mesmo paciente");

        String consultas = paciente.get("/api/paciente/consultas").body();
        assertTrue(consultas.contains("\"especialidade\":\"Clínico geral\""), consultas);
        assertTrue(consultas.contains("\"podeCancelar\":true"));

        assertEquals(200, paciente.post("/api/paciente/consultas/cancelar", "id", "1").statusCode());
        assertTrue(paciente.get("/api/paciente/consultas").body().contains("\"status\":\"CANCELADA\""));
    }

    @Test
    void responsavelDeveVerSoSeusPacientesEOAlertaDoLembretePerdido() throws Exception {
        Navegador maria = new Navegador();
        maria.cadastrar("Maria Souza", "maria@teste.com", "PACIENTE");
        Navegador outro = new Navegador();
        outro.cadastrar("Pedro Lima", "pedro@teste.com", "PACIENTE");
        Navegador joao = new Navegador();
        joao.cadastrar("Joao Souza", "joao@teste.com", "RESPONSAVEL");

        assertEquals(200, maria.post("/api/paciente/responsaveis", "email", "joao@teste.com").statusCode());
        assertEquals(409, maria.post("/api/paciente/responsaveis", "email", "joao@teste.com").statusCode());
        assertEquals(400, maria.post("/api/paciente/responsaveis", "email", "pedro@teste.com").statusCode(),
                "pedro e paciente, nao responsavel");

        LocalDate hoje = AGORA.toLocalDate();
        maria.post("/api/paciente/medicamentos", "nome", "Losartana", "dosagem", "50 mg", "frequencia", "24",
                "primeiroHorario", "10:00", "inicio", hoje.toString(), "fim", hoje.plusDays(7).toString());

        // Simula o agendador: gera o lembrete das 10:00, deixa o prazo passar e alerta
        Long mariaId = servicos.usuarios().buscarPorEmail("maria@teste.com").getId();
        servicos.gerador().gerarDoDia(mariaId, hoje);
        assertTrue(maria.get("/api/paciente/agenda").body().contains("\"agora\":{"), "cartao Agora");
        relogio.avancar(Duration.ofMinutes(31));
        for (Lembrete perdido : servicos.lembretes().verificarPerdidos(LocalDateTime.now(relogio))) {
            servicos.alertas().alertarResponsaveis(perdido);
        }

        String pacientes = joao.get("/api/responsavel/pacientes").body();
        assertTrue(pacientes.contains("Maria Souza"), pacientes);
        assertFalse(pacientes.contains("Pedro Lima"), "nao vinculado");
        assertTrue(pacientes.contains("\"perdidosHoje\":1"));
        assertTrue(pacientes.contains("\"alertaAs\":\"10:31\""), pacientes);

        String historico = maria.get("/api/paciente/historico").body();
        assertTrue(historico.contains("\"status\":\"PERDIDO\""));
        assertTrue(historico.contains("\"responsaveis\":[\"Joao\"]"), historico);

        assertEquals(200, maria.post("/api/paciente/responsaveis/remover", "email", "joao@teste.com").statusCode());
        assertFalse(joao.get("/api/responsavel/pacientes").body().contains("Maria Souza"));
    }

    @Test
    void jsonDeveEscaparTextoComSeguranca() {
        String json = Json.escrever(Json.objeto("texto", "a\"b\\c\n</script>", "nulo", null,
                "lista", java.util.List.of(1, true), "data", LocalDate.of(2026, 11, 23)));
        assertEquals("{\"texto\":\"a\\\"b\\\\c\\n\\u003c/script\\u003e\",\"nulo\":null,"
                + "\"lista\":[1,true],\"data\":\"2026-11-23\"}", json);
    }

    @Test
    void parametrosDevemDecodificarFormularioEmUtf8() {
        Parametros p = Parametros.de("nome=Cl%C3%ADnico+geral&vazio=", "id=7");
        assertEquals("Clínico geral", p.texto("nome"));
        assertTrue(p.opcional("vazio").isEmpty());
        assertEquals(7L, p.id("id"));
    }
}
