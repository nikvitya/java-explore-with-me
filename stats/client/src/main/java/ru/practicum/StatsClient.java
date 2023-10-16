package ru.practicum;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatsClient {
    final WebClient webClient;

    public StatsClient(@Value("${stats-server.url}") String connectionURL) {
        webClient = WebClient.create(connectionURL);
    }

    public StatsHitDto save(StatsHitDto statsHitDto) {
        return webClient.post()
                .uri("/hit")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(statsHitDto), StatsHitDto.class)
                .retrieve()
                .bodyToMono(StatsHitDto.class)
                .block();
    }

    public Flux<StatsResponseDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/stats")
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("uris", uris)
                .queryParam("unique", unique);

        return webClient.get()
                .uri(uriBuilder.build().toUri())
                .retrieve()
                .bodyToFlux(StatsResponseDto.class);
    }
}