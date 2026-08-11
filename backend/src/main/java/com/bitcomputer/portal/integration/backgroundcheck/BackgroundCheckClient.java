package com.bitcomputer.portal.integration.backgroundcheck;

import com.bitcomputer.portal.exception.AppException;
import com.bitcomputer.portal.integration.backgroundcheck.BackgroundCheckDtos.*;
import io.netty.channel.ChannelOption;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Component
public class BackgroundCheckClient {
    private final WebClient webClient;

    public BackgroundCheckClient(BackgroundCheckProperties properties, WebClient.Builder builder) {
        var httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis())
                .responseTimeout(properties.readTimeout());
        this.webClient = builder.baseUrl(properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public Created create(CreateRequest request) {
        return execute(webClient.post().uri("/background-checks").bodyValue(request), Created.class);
    }

    public History history(String employeeNumber) {
        return execute(webClient.get().uri(uri -> uri.path("/background-checks")
                .queryParam("employeeId", employeeNumber).build()), History.class);
    }

    public Result get(String checkId) {
        return execute(webClient.get().uri("/background-checks/{checkId}", checkId), Result.class);
    }

    private <T> T execute(WebClient.RequestHeadersSpec<?> request, Class<T> type) {
        try {
            return request.exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) return response.bodyToMono(type);
                        return response.bodyToMono(ExternalError.class)
                                .defaultIfEmpty(new ExternalError(null, null, null, response.statusCode().value()))
                                .flatMap(error -> Mono.error(mapError(response.statusCode().value(), error)));
                    })
                    .timeout(java.time.Duration.ofSeconds(10))
                    .block();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            var cause = e.getCause();
            var timedOut = e instanceof TimeoutException || cause instanceof TimeoutException
                    || e.getClass().getSimpleName().toLowerCase().contains("timeout");
            throw new AppException(HttpStatus.BAD_GATEWAY,
                    timedOut ? "BACKGROUND_CHECK_TIMEOUT" : "BACKGROUND_CHECK_CONNECTION_FAILED",
                    timedOut ? "Background Check 서비스 응답이 지연되고 있습니다. 다시 시도해 주세요."
                            : "Background Check 서비스에 연결할 수 없습니다.");
        }
    }

    private AppException mapError(int status, ExternalError error) {
        return switch (status) {
            case 400 -> new AppException(HttpStatus.BAD_REQUEST, "BACKGROUND_CHECK_BAD_REQUEST",
                    safe(error.message(), "Background Check 요청이 올바르지 않습니다."));
            case 404 -> new AppException(HttpStatus.NOT_FOUND, "BACKGROUND_CHECK_NOT_FOUND",
                    "Background Check 결과를 찾을 수 없습니다.");
            case 503 -> new AppException(HttpStatus.SERVICE_UNAVAILABLE, "BACKGROUND_CHECK_UNAVAILABLE",
                    "Background Check 서비스가 일시적으로 사용할 수 없습니다.", error.retryAfter());
            default -> new AppException(HttpStatus.BAD_GATEWAY, "BACKGROUND_CHECK_ERROR",
                    "Background Check 서비스에서 오류가 발생했습니다.");
        };
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
