package ru.practicum.service;

import ru.practicum.StatsHitDto;
import ru.practicum.StatsResponseDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {
    StatsHitDto save(StatsHitDto statsHitDto);

    List<StatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);

}
