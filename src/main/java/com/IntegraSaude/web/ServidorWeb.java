package com.IntegraSaude.web;

import com.IntegraSaude.exception.AppException;
import com.IntegraSaude.exception.CredenciaisInvalidasException;
import com.IntegraSaude.exception.DadoInvalidoException;
import com.IntegraSaude.exception.OperacaoInvalidaException;
import com.IntegraSaude.exception.RegistroDuplicadoException;
import com.IntegraSaude.exception.RegistroNaoEncontradoException;
import com.IntegraSaude.exception.ServicoExternoException;
import com.IntegraSaude.service.Servicos;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor do frontend web, feito só com o com.sun.net.httpserver do JDK.
 * Serve os arquivos estáticos (src/main/resources/web) e uma API JSON em /api/ que apenas
 * chama os serviços existentes: nenhuma regra de negócio mora aqui.
 *
 * Escuta só em localhost. Requisições POST precisam do cabeçalho X-Requested-With: fetch,
 * o que, junto do cookie SameSite=Strict, impede que outro site dispare ações (CSRF).
 */
public class ServidorWeb {

    private static final Logger LOGGER = Logger.getLogger(ServidorWeb.class.getName());

    private static final int OK = 200;
    private static final int REQUISICAO_INVALIDA = 400;
    private static final int NAO_ENCONTRADO = 404;
    private static final int METODO_NAO_PERMITIDO = 405;
    private static final int CONFLITO = 409;
    private static final int ERRO_INTERNO = 500;
    private static final int FALHA_SERVICO_EXTERNO = 502;

    private static final int LIMITE_CORPO_BYTES = 64 * 1024;
    private static final String CABECALHO_FETCH = "X-Requested-With";
    private static final String PASTA_ARQUIVOS = "/web/";

    // Caminho público -> arquivo em src/main/resources/web e tipo de conteúdo
    private static final Map<String, String[]> ARQUIVOS = Map.of(
            "/", new String[] {"index.html", "text/html; charset=utf-8"},
            "/index.html", new String[] {"index.html", "text/html; charset=utf-8"},
            "/estilo.css", new String[] {"estilo.css", "text/css; charset=utf-8"},
            "/app.js", new String[] {"app.js", "text/javascript; charset=utf-8"},
            "/icone.svg", new String[] {"icone.svg", "image/svg+xml"});

    // Só a própria página e as fontes do Google podem ser carregadas
    private static final String POLITICA_CONTEUDO = "default-src 'self'; "
            + "style-src 'self' https://fonts.googleapis.com; font-src https://fonts.gstatic.com; "
            + "img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'";

    private final HttpServer servidor;
    private final Servicos servicos;
    private final SessoesWeb sessoes;
    private final Map<String, Rota> rotas = new HashMap<>();

    public ServidorWeb(Servicos servicos, int porta) {
        this.servicos = servicos;
        this.sessoes = new SessoesWeb(servicos.relogio());
        VisaoLembretes visao = new VisaoLembretes(servicos);
        new ApiConta(servicos).registrar(this);
        new ApiPaciente(servicos, visao).registrar(this);
        new ApiResponsavel(servicos, visao).registrar(this);
        try {
            servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), porta), 0);
        } catch (IOException e) {
            throw new OperacaoInvalidaException("Nao foi possivel abrir o servidor web na porta " + porta
                    + " (ela ja esta em uso?)", e);
        }
        servidor.createContext("/api/", this::tratarApi);
        servidor.createContext("/", this::tratarArquivo);
    }

    void get(String caminho, Rota rota) {
        rotas.put("GET " + caminho, rota);
    }

    void post(String caminho, Rota rota) {
        rotas.put("POST " + caminho, rota);
    }

    public void iniciar() {
        servidor.start();
    }

    // Para de aceitar conexões e espera no máximo 1 segundo pelas que estão em andamento
    public void parar() {
        servidor.stop(1);
    }

    public int porta() {
        return servidor.getAddress().getPort();
    }

    public String endereco() {
        return "http://localhost:" + porta() + "/";
    }

    // ---------------------------------------------------------------- API JSON

    private void tratarApi(HttpExchange troca) throws IOException {
        try {
            String metodo = troca.getRequestMethod();
            Rota rota = rotas.get(metodo + " " + troca.getRequestURI().getPath());
            if (rota == null) {
                responderJson(troca, NAO_ENCONTRADO, erro("Rota nao encontrada"));
                return;
            }
            boolean ehPost = "POST".equals(metodo);
            if (ehPost && !"fetch".equals(troca.getRequestHeaders().getFirst(CABECALHO_FETCH))) {
                responderJson(troca, AcessoNegadoException.PROIBIDO, erro("Requisicao recusada"));
                return;
            }
            Parametros parametros = Parametros.de(troca.getRequestURI().getRawQuery(),
                    ehPost ? lerCorpo(troca) : null);
            Object resultado = rota.atender(new Requisicao(troca, parametros, sessoes, servicos.usuarios()));
            responderJson(troca, OK, resultado == null ? Json.objeto("ok", true) : resultado);
        } catch (AcessoNegadoException e) {
            responderJson(troca, e.getStatus(), erro(e.getMessage()));
        } catch (AppException e) {
            int status = statusDe(e);
            if (status == ERRO_INTERNO) {
                LOGGER.log(Level.SEVERE, "Falha ao atender " + troca.getRequestURI(), e);
            }
            responderJson(troca, status, erro(e.getMessage()));
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Erro inesperado em " + troca.getRequestURI(), e);
            responderJson(troca, ERRO_INTERNO, erro("Erro inesperado. Tente novamente."));
        } finally {
            troca.close();
        }
    }

    // Cada exceção própria do sistema vira o código HTTP correspondente
    private static int statusDe(AppException e) {
        return switch (e) {
            case CredenciaisInvalidasException ignorada -> AcessoNegadoException.SEM_LOGIN;
            case DadoInvalidoException ignorada -> REQUISICAO_INVALIDA;
            case OperacaoInvalidaException ignorada -> REQUISICAO_INVALIDA;
            case RegistroNaoEncontradoException ignorada -> NAO_ENCONTRADO;
            case RegistroDuplicadoException ignorada -> CONFLITO;
            case ServicoExternoException ignorada -> FALHA_SERVICO_EXTERNO;
            default -> ERRO_INTERNO;
        };
    }

    private static Map<String, Object> erro(String mensagem) {
        return Json.objeto("erro", mensagem);
    }

    // Lê o corpo do formulário com limite de tamanho
    private static String lerCorpo(HttpExchange troca) throws IOException {
        try (InputStream entrada = troca.getRequestBody()) {
            byte[] bytes = entrada.readNBytes(LIMITE_CORPO_BYTES + 1);
            if (bytes.length > LIMITE_CORPO_BYTES) {
                throw new DadoInvalidoException("Requisicao muito grande");
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static void responderJson(HttpExchange troca, int status, Object corpo) throws IOException {
        responder(troca, status, "application/json; charset=utf-8",
                Json.escrever(corpo).getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------- Arquivos estáticos

    private void tratarArquivo(HttpExchange troca) throws IOException {
        try {
            String metodo = troca.getRequestMethod();
            if (!"GET".equals(metodo) && !"HEAD".equals(metodo)) {
                responderTexto(troca, METODO_NAO_PERMITIDO, "Metodo nao permitido");
                return;
            }
            String[] arquivo = ARQUIVOS.get(troca.getRequestURI().getPath());
            byte[] conteudo = arquivo == null ? null : ler(arquivo[0]);
            if (conteudo == null) {
                responderTexto(troca, NAO_ENCONTRADO, "Pagina nao encontrada");
                return;
            }
            responder(troca, OK, arquivo[1], "HEAD".equals(metodo) ? null : conteudo);
        } finally {
            troca.close();
        }
    }

    private static byte[] ler(String nome) throws IOException {
        try (InputStream entrada = ServidorWeb.class.getResourceAsStream(PASTA_ARQUIVOS + nome)) {
            return entrada == null ? null : entrada.readAllBytes();
        }
    }

    private static void responderTexto(HttpExchange troca, int status, String texto) throws IOException {
        responder(troca, status, "text/plain; charset=utf-8", texto.getBytes(StandardCharsets.UTF_8));
    }

    // Envia a resposta com cabeçalhos de segurança; corpo nulo = resposta sem corpo (HEAD)
    private static void responder(HttpExchange troca, int status, String tipo, byte[] corpo) throws IOException {
        Headers cabecalhos = troca.getResponseHeaders();
        cabecalhos.set("Content-Type", tipo);
        cabecalhos.set("Cache-Control", "no-store");
        cabecalhos.set("X-Content-Type-Options", "nosniff");
        cabecalhos.set("Referrer-Policy", "no-referrer");
        cabecalhos.set("Content-Security-Policy", POLITICA_CONTEUDO);
        if (corpo == null) {
            troca.sendResponseHeaders(status, -1);
            return;
        }
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }
}
