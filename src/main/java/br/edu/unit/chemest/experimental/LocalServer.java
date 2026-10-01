package br.edu.unit.chemest.experimental;
import br.edu.unit.chemest.io.GaussianOutputParser;
import br.edu.unit.chemest.model.OrbitalResult;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/** Local web interface. Uploaded data is streamed to disk, never buffered as a whole. */
public final class
LocalServer {
    private String origin;

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        var executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            executor.close();
        }));
        server.start();
        System.out.println("Analisador HOMO-LUMO: " + origin);
        System.out.println("Encerre a execução Java para fechar o servidor local.");
        if (!Boolean.getBoolean("analyzer.noBrowser")) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(origin));
                }
            } catch (IOException | RuntimeException e) {
                System.out.println("Abra o endereço acima no navegador.");
            }
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Content-Security-Policy",
                    "default-src 'self'; style-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/analisar")) {
                if (!exchange.getRequestMethod().equals("POST")) {
                    reply(exchange, 405, "text/plain", "Método não permitido.");
                    return;
                }
                if (!origin.equals(exchange.getRequestHeaders().getFirst("Origin"))) {
                    reply(exchange, 403, "text/plain", "Origem não permitida.");
                    return;
                }
                analyze(exchange);
                return;
            }
            Map<String, String> assets = Map.of("/", "index.html", "/styles.css", "styles.css", "/app.js", "app.js");
            String asset = assets.get(path);
            if (asset == null || !exchange.getRequestMethod().equals("GET")) {
                reply(exchange, 404, "text/plain", "Página não encontrada.");
                return;
            }
            try (var input = LocalServer.class.getResourceAsStream("/web/" + asset)) {
                if (input == null) {
                    reply(exchange, 500, "text/plain", "Recursos da interface não encontrados.");
                    return;
                }
                String type = asset.endsWith("css") ? "text/css" : asset.endsWith("js") ? "text/javascript" : "text/html";
                reply(exchange, 200, type, new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    private void analyze(HttpExchange exchange) throws IOException {
        Path temporary = null;
        try {
            temporary = Files.createTempFile("homo-lumo-", ".out");
            Files.copy(exchange.getRequestBody(), temporary, StandardCopyOption.REPLACE_EXISTING);
            OrbitalResult result = new GaussianOutputParser().parse(temporary);
            // JSON cannot represent infinities; the scientific parser remains unchanged.
            if (!Double.isFinite(result.homoHartree()) || !Double.isFinite(result.lumoHartree())
                    || !Double.isFinite(result.gapHartree()) || !Double.isFinite(result.gapEv())) {
                throw new NumberFormatException();
            }
            reply(exchange, 200, "application/json", String.format(Locale.ROOT,
                    "{\"homo\":%.12f,\"lumo\":%.12f,\"gapHartree\":%.12f,\"gapEv\":%.12f}",
                    result.homoHartree(), result.lumoHartree(), result.gapHartree(), result.gapEv()));
        } catch (NumberFormatException e) {
            reply(exchange, 400, "text/plain", "O arquivo contém valores de energia inválidos.");
        } catch (IllegalArgumentException e) {
            reply(exchange, 400, "text/plain", "Não foi possível localizar os valores HOMO e LUMO.");
        } catch (IOException e) {
            reply(exchange, 500, "text/plain", "Erro durante a leitura do arquivo.");
        } finally {
            if (temporary != null) Files.deleteIfExists(temporary);
        }
    }

    private static void reply(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}

