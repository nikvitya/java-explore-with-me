package ru.practicum.mapper;

import ru.practicum.StatsHitDto;
import ru.practicum.StatsResponseDto;
import ru.practicum.model.StatsHit;
import ru.practicum.model.StatsResponse;

public class Mapper {
    public static StatsHitDto toHitDto(StatsHit statsHit) {
        StatsHitDto statsHitDto = new StatsHitDto();
        statsHitDto.setIp(statsHit.getIp());
        statsHitDto.setApp(statsHit.getApp());
        statsHitDto.setUri(statsHit.getUri());
        statsHitDto.setTimestamp(statsHit.getTimestamp());
        return statsHitDto;
    }

    public static StatsHit toHit(StatsHitDto statsHitDto) {
        StatsHit statsHit = new StatsHit();
        statsHit.setIp(statsHitDto.getIp());
        statsHit.setApp(statsHitDto.getApp());
        statsHit.setUri(statsHitDto.getUri());
        statsHit.setTimestamp(statsHitDto.getTimestamp());
        return statsHit;
    }

    public static StatsResponseDto toStatsDto(StatsResponse stats) {
        StatsResponseDto statsResponseDto = new StatsResponseDto();
        statsResponseDto.setApp(stats.getApp());
        statsResponseDto.setHits(stats.getHits());
        statsResponseDto.setUri(stats.getUri());
        return statsResponseDto;
    }
}