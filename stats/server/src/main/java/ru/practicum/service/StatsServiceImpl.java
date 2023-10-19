package ru.practicum.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsHitDto;
import ru.practicum.StatsResponseDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.mapper.Mapper;
import ru.practicum.model.StatsHit;
import ru.practicum.model.StatsResponse;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {

    final StatsRepository statsRepository;

    @Transactional
    public StatsHitDto save(StatsHitDto statsHitDto) {
        log.info("Сохранение статистики..");
        StatsHit statsHit = Mapper.toHit(statsHitDto);
        StatsHit savedStatsHit = statsRepository.save(statsHit);
        return Mapper.toHitDto(savedStatsHit);
    }

    public List<StatsResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new BadRequestException("Неверно указано время начала");
        }

        List<StatsResponse> statsList;
        log.info("Получение статистики..");
        if (unique) {
            if (uris != null) {
                statsList = statsRepository.findUniqueStats(start, end, uris);
            } else {
                statsList = statsRepository.findUniqueStats(start, end);
            }
        } else {
            if (uris != null) {
                statsList = statsRepository.findStats(start, end, uris);
            } else {
                statsList = statsRepository.findStats(start, end);
            }
        }

        List<StatsResponseDto> statsResponseDtoList = statsList.stream()
                .map(Mapper::toStatsDto)
                .collect(Collectors.toList());

        return statsResponseDtoList;
    }
}