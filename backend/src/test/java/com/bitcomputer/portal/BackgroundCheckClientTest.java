package com.bitcomputer.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bitcomputer.portal.exception.AppException;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckClient;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.CreateRequest;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class BackgroundCheckClientTest {
    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void start() throws Exception {
        requests.set(0);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void mapsCreatedResponse() {
        respond(201, """
                {"checkId":"CHK-1","employeeId":"EMP-001","status":"pending",
                 "createdAt":"2025-01-15T09:30:00Z","message":"ok"}
                """);
        var result = client().create(new CreateRequest("EMP-001", "민준", "김",
                LocalDate.of(1990, 3, 15)));
        assertThat(result.checkId()).isEqualTo("CHK-1");
        assertThat(result.status()).isEqualTo("pending");
    }

    @Test
    void mapsServiceUnavailableAndRetryAfter() {
        respond(503, """
                {"error":"Service Unavailable","message":"busy","retryAfter":30,"statusCode":503}
                """);
        var client = client();
        assertThatThrownBy(() -> client.history("EMP-001"))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.code()).isEqualTo("BACKGROUND_CHECK_UNAVAILABLE");
                    assertThat(e.retryAfter()).isEqualTo(30);
                });
        assertThatThrownBy(() -> client.history("EMP-001"))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.retryAfter()).isPositive());
        assertThat(requests).hasValue(1);
    }

    private BackgroundCheckClient client() {
        var props = new BackgroundCheckProperties("http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1), Duration.ofSeconds(2));
        return new BackgroundCheckClient(props, WebClient.builder());
    }

    private void respond(int status, String body) {
        server.createContext("/background-checks", exchange -> {
            requests.incrementAndGet();
            var bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }
}
