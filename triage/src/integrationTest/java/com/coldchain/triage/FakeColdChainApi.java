package com.coldchain.triage;

import com.coldchain.triage.internal.infrastructure.coldchainapi.ApiAnswers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class FakeColdChainApi {

    static final String SECRET = "triage-integration-secret";

    private static final String TOKEN = "fake-platform-token";

    private static final long TOKEN_LIFETIME_SECONDS = 900;

    private final HttpServer server;

    private final Map<UUID, Integer> allowances = new ConcurrentHashMap<>();

    private final Set<UUID> down = ConcurrentHashMap.newKeySet();

    private FakeColdChainApi(HttpServer server) {
        this.server = server;
    }

    static FakeColdChainApi start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            FakeColdChainApi api = new FakeColdChainApi(server);
            server.createContext("/api/v1/auth/client-token", api::token);
            server.createContext("/api/internal/v1/shipments/", api::context);
            server.start();
            return api;
        } catch (IOException cause) {
            throw new UncheckedIOException("Could not start the fake API", cause);
        }
    }

    String baseUrl() {
        return "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort()
                + "/api";
    }

    void knows(UUID shipment, int maxSingleExcursionMinutes) {
        allowances.put(shipment, maxSingleExcursionMinutes);
    }

    void isDownFor(UUID shipment) {
        down.add(shipment);
    }

    void stop() {
        server.stop(0);
    }

    private void token(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        if (!body.contains(SECRET)) {
            answer(exchange, 401, "{}");
            return;
        }
        answer(exchange, 201, ApiAnswers.token(TOKEN, TOKEN_LIFETIME_SECONDS));
    }

    private void context(HttpExchange exchange) throws IOException {
        if (!("Bearer " + TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            answer(exchange, 401, "{}");
            return;
        }
        UUID shipment = UUID.fromString(exchange.getRequestURI().getPath().split("/")[5]);
        if (down.contains(shipment)) {
            answer(exchange, 503, "{}");
            return;
        }
        Integer allowance = allowances.get(shipment);
        if (allowance == null) {
            answer(exchange, 404, "{}");
            return;
        }
        answer(exchange, 200, ApiAnswers.context(allowance));
    }

    private static void answer(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
