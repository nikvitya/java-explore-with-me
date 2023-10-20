package ru.practicum.compilation.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.CompilationDtoMapper;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.compilation.repository.CompilationRepository;
import ru.practicum.event.dto.EventDtoMapper;
import ru.practicum.event.dto.EventShortDto;
import ru.practicum.event.model.Event;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompilationServiceImpl implements CompilationService {

    final CompilationRepository compilationRepository;
    final CompilationDtoMapper compilationDtoMapper;
    final EventRepository eventRepository;

    @Transactional
    @Override
    public CompilationDto add(NewCompilationDto compilationDto) {
        log.info("Добавление подборки");
        List<Event> events = getEventsFromDto(compilationDto);
        Compilation compilation = createOrUpdateCompilation(compilationDto, events);
        CompilationDto result = compileDtoWithEvents(compilation);
        return result;
    }

    @Transactional
    @Override
    public CompilationDto update(Long compId, UpdateCompilationRequest compilationDto) {
        log.info("Обновление подборки, id={}", compId);
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException("Подборка не существует " + compId));

        List<Event> events = getEventsFromDto(compilationDto);
        compilation.setEvents(events);

        if (compilationDto.getTitle() != null) {
            compilation.setTitle(compilationDto.getTitle());
        }
        if (compilationDto.getPinned() != null) {
            compilation.setPinned(compilationDto.getPinned());
        }

        compilation = createOrUpdateCompilation(compilation);
        CompilationDto result = compileDtoWithEvents(compilation);
        return result;
    }

    @Transactional
    @Override
    public void delete(Long compId) {
        log.info("Удаление подборки, id={}", compId);
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException("Подборка не существует " + compId));
        compilationRepository.deleteById(compId);
        log.info("Подборка удалена " + compId);
    }

    @Override
    public List<CompilationDto> getAll(Boolean pinned, Pageable page) {
        log.info("Получение подборок событий");
        List<Compilation> compilations = getCompilationsByPinned(pinned, page);
        List<CompilationDto> compilationDtos = compileDtosWithEvents(compilations);
        return compilationDtos;
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение информации о подборке, id={}", compId);
        Compilation compilation = compilationRepository.findById(compId).orElseThrow(
                () -> new NotFoundException("Подборка не найдена " + compId)
        );
        CompilationDto result = compileDtoWithEvents(compilation);
        return result;
    }

    private List<Event> getEventsFromDto(NewCompilationDto compilationDto) {
        List<Event> events = new ArrayList<>();
        if (compilationDto.getEvents() != null) {
            events = eventRepository.findAllByIdIn(compilationDto.getEvents());
        }
        return events;
    }

    private List<Event> getEventsFromDto(UpdateCompilationRequest compilationDto) {
        List<Event> events = new ArrayList<>();
        if (compilationDto.getEvents() != null) {
            events = eventRepository.findAllByIdIn(compilationDto.getEvents());
        }
        return events;
    }

    private Compilation createOrUpdateCompilation(NewCompilationDto compilationDto, List<Event> events) {
        Compilation compilation = compilationDtoMapper.mapNewCompilationDtoToCompilation(compilationDto, events);
        if (compilation.getPinned() == null) {
            compilation.setPinned(false);
        }
        return compilationRepository.save(compilation);
    }

    private Compilation createOrUpdateCompilation(Compilation compilation) {
        return compilationRepository.save(compilation);
    }

    private List<Compilation> getCompilationsByPinned(Boolean pinned, Pageable page) {
      return compilationRepository.findAllByPinned(pinned, page);
    }

    private List<CompilationDto> compileDtosWithEvents(List<Compilation> compilations) {

        return compilations.stream()
                .map(this::compileDtoWithEvents)
                .collect(Collectors.toList());
    }

    private CompilationDto compileDtoWithEvents(Compilation compilation) {
        CompilationDto result = compilationDtoMapper.mapCompilationToDto(compilation);
        List<EventShortDto> eventDtos = new ArrayList<>();
        for (Event event : compilation.getEvents()) {
            eventDtos.add(EventDtoMapper.mapEventToShortDto(event));
        }
        result.setEvents(eventDtos);
        return result;
    }
}
