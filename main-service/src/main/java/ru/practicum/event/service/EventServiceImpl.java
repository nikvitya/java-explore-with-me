package ru.practicum.event.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.StatsClient;
import ru.practicum.StatsHitDto;
import ru.practicum.StatsResponseDto;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.dto.*;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.model.StateAction;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.event.repository.LocationRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.WrongDataException;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.util.ErrorMessages.CATEGORY_NOT_FOUND;
import static ru.practicum.util.JsonFormatPattern.JSON_FORMAT_PATTERN_FOR_TIME;
import static ru.practicum.util.LogMessages.EVENT_SAVED;
import static ru.practicum.util.LogMessages.EVENT_SEARCHING;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventServiceImpl implements EventService {
    final EventRepository eventRepository;
    final UserRepository userRepository;
    final CategoryRepository categoryRepository;
    final RequestRepository requestRepository;
    final StatsClient statsClient;
    final LocationRepository locationRepository;

    @Override
    @Transactional
    public EventFullDto add(Long userId, NewEventDto newEvent) {
        log.info("Добавление нового события пользователем {}", userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Category category = categoryRepository.findById(newEvent.getCategory()).orElseThrow(
                () -> new NotFoundException(CATEGORY_NOT_FOUND + newEvent.getCategory())
        );

        Event event = EventDtoMapper.mapNewEventDtoToEvent(newEvent, category);
        saveLocation(event);
        event.setInitiator(user);
        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);
        Long confirmedRequests = requestRepository.countByEventAndStatuses(event.getId(), List.of("CONFIRMED"));
        if (event.getPaid() == null) {
            event.setPaid(false);
        }
        if (event.getParticipantLimit() == null) {
            event.setParticipantLimit(0);
        }
        if (event.getRequestModeration() == null) {
            event.setRequestModeration(true);
        }
        if (LocalDateTime.now().isAfter(event.getEventDate().minus(2, ChronoUnit.HOURS))) {
            throw new WrongDataException("До начала события меньше часа, изменение невозможно");
        }
        event = eventRepository.save(event);
        log.info(EVENT_SAVED + event.getId());
        EventFullDto eventFullDto = EventDtoMapper.mapEventToFullDto(event, confirmedRequests);
        return eventFullDto;
    }

    @Override
    public List<EventFullDto> get(List<Long> users, List<String> states, List<Long> categories, String rangeStart, String rangeEnd, Integer from, Integer size) {
        log.info(EVENT_SEARCHING);
        LocalDateTime rangeStartDateTime = null;
        LocalDateTime rangeEndDateTime = null;
        if (rangeStart != null) {
            rangeStartDateTime = LocalDateTime.parse(rangeStart, DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
        }
        if (rangeEnd != null) {
            rangeEndDateTime = LocalDateTime.parse(rangeEnd, DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
            if (rangeStartDateTime.isAfter(rangeEndDateTime)) {
                throw new WrongDataException("Дата и время начала поиска не должна быть позже даты и времени конца поиска");
            }
        }
        List<EventState> eventStateList;
        if (states != null) {
            eventStateList = states.stream().map(EventState::valueOf).collect(Collectors.toList());
        } else {
            eventStateList = Arrays.stream(EventState.values()).collect(Collectors.toList());
        }
        List<EventFullDto> dtos;

        if (users == null && categories == null) {
            ArrayList<Event> events = new ArrayList<>(eventRepository.findAll(PageRequest.of(from / size, size)).getContent());
            List<ParticipationRequest> requestsByEventIds = requestRepository.findByEventIds(events.stream()
                    .mapToLong(e -> e.getId()).boxed().collect(Collectors.toList()));
            dtos = events.stream()
                    .map(e -> EventDtoMapper.mapEventToFullDto(e,
                            requestsByEventIds.stream()
                                    .filter(r -> r.getEvent().getId().equals(e.getId()))
                                    .count()))
                    .collect(Collectors.toList());
        } else {
            List<Event> allEventsWithDates = new ArrayList<>(eventRepository.findAllEventsWithDates(users,
                    eventStateList, categories, rangeStartDateTime, rangeEndDateTime, PageRequest.of(from / size, size)));
            List<ParticipationRequest> requestsByEventIds = requestRepository.findByEventIds(allEventsWithDates.stream()
                    .mapToLong(e -> e.getId()).boxed().collect(Collectors.toList()));
            dtos = allEventsWithDates.stream()
                    .map(e -> EventDtoMapper.mapEventToFullDto(e,
                            requestsByEventIds.stream()
                                    .filter(r -> r.getEvent().getId().equals(e.getId()))
                                    .count()))
                    .collect(Collectors.toList());
        }
        dtos = getViewCounters(dtos);
        return dtos;
    }

    @Override
    @Transactional
    public EventFullDto update(Long eventId, UpdateEventAdminRequest updateRequest) {
        log.info("Редактирование данных события и его статуса");
        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие не существует " + eventId));

        if (LocalDateTime.now().isAfter(event.getEventDate().minus(2, ChronoUnit.HOURS))) {
            throw new ConflictException("До начала события меньше часа, изменение события невозможно");
        }
        if (!event.getState().equals(EventState.PENDING)) {
            throw new ConflictException("Событие не в состоянии \"Ожидание публикации\", изменение события невозможно");
        }
        if ((!StateAction.REJECT_EVENT.toString().equals(updateRequest.getStateAction())
                && event.getState().equals(EventState.PUBLISHED))) {
            throw new ConflictException("Отклонить опубликованное событие невозможно");
        }
        updateEventWithAdminRequest(event, updateRequest);
        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new WrongDataException("Событие уже завершилось");
        }
        saveLocation(event);
        event = eventRepository.save(event);
        EventFullDto eventFullDto = getEventFullDto(event);
        return getViewsCounter(eventFullDto);
    }

    @Override
    public List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size) {
        log.info("Получение событий, добавленных пользователем {}", userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        return eventRepository.findAllByInitiator(user, PageRequest.of(from / size, size)).stream()
                .map(EventDtoMapper::mapEventToShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventOfUserByIds(Long userId, Long eventId) {
        log.info("Получение полной информации о событии {} пользователем {}", eventId, userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Event event = getEventById(eventId);
        if (!user.getId().equals(event.getInitiator().getId())) {
            throw new WrongDataException("Пользователь " + userId + " не является инициатором события " + eventId);
        }
        EventFullDto eventFullDto = getEventFullDto(event);
        return getViewsCounter(eventFullDto);
    }

    @Override
    public EventFullDto updateEventOfUserByIds(Long userId, Long eventId, UpdateEventUserRequest request) {
        log.info("Изменение события {} пользователем {}", eventId, userId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Event event = getEventById(eventId);
        if (!user.getId().equals(event.getInitiator().getId())) {
            throw new ConflictException("Пользователь " + userId + " не инициатор события " + eventId);
        }
        event = updateEventWithUserRequest(event, request);
        saveLocation(event);
        eventRepository.save(event);
        EventFullDto eventFullDto = getEventFullDto(event);
        return getViewsCounter(eventFullDto);
    }

    @Override
    public EventFullDto getEventDtoById(Long eventId, String uri, String ip) {
        log.info("Получение подробной информации об опубликованном событии по его идентификатору");
        statsClient.saveHit(new StatsHitDto("ewm-service",
                uri,
                ip,
                LocalDateTime.now()));
        Event event = getEventById(eventId);
        if (!event.getState().equals(EventState.PUBLISHED) && !uri.toLowerCase().contains("admin")) {
            throw new NotFoundException("Такого события не существует");
        }
        EventFullDto eventFullDto = getEventFullDto(event);
        return getViewsCounter(eventFullDto);
    }

    @Override
    public List<EventShortDto> getEventsWithFilters(String text, List<Long> categories, Boolean paid,
                                                    String rangeStart, String rangeEnd, Boolean onlyAvailable,
                                                    String sort, Integer from, Integer size, String uri, String ip) {
        log.info("Получение событий с возможностью фильтрации");
        List<Event> events;
        LocalDateTime startDate;
        LocalDateTime endDate;
        Boolean sortDate = sort.equals("EVENT_DATE");
        if (sortDate) {
            if (rangeStart == null && rangeEnd == null && categories != null) {
                events = eventRepository.findAllByCategoryIdPageable(categories, EventState.PUBLISHED, PageRequest.of(from / size, size));
            } else {
                if (rangeStart == null) {
                    startDate = LocalDateTime.now();
                } else {
                    startDate = LocalDateTime.parse(rangeStart, DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
                }
                if (text == null) {
                    text = "";
                }
                if (rangeEnd == null) {
                    events = eventRepository.findEventsByText(text.toLowerCase(), EventState.PUBLISHED, PageRequest.of(from / size, size));
                } else {
                    endDate = LocalDateTime.parse(rangeEnd, DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
                    if (startDate.isAfter(endDate)) {
                        throw new WrongDataException("Дата и время начала поиска не должна быть позже даты и времени конца поиска");
                    } else {
                        events = eventRepository.findAllByTextAndDateRange(text.toLowerCase(),
                                startDate,
                                endDate,
                                EventState.PUBLISHED,
                                PageRequest.of(from / size, size));
                    }
                }
            }
        } else {
            if (rangeStart == null) {
                startDate = LocalDateTime.now();
            } else {
                startDate = LocalDateTime.parse(rangeStart, DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
            }
            if (rangeEnd == null) {
                endDate = null;
            } else {
                endDate = LocalDateTime.parse(rangeEnd, DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
            }
            if (rangeStart != null && rangeEnd != null) {
                if (startDate.isAfter(endDate)) {
                    throw new WrongDataException("Дата и время начала поиска не должна быть позже даты и времени конца поиска");
                }
            }
            events = eventRepository.findEventList(text, categories, paid, startDate, endDate, EventState.PUBLISHED);
        }
        statsClient.saveHit(new StatsHitDto("ewm-service",
                uri,
                ip,
                LocalDateTime.now()));
        if (!sortDate) {
            List<EventShortDto> shortEventDtos = createShortEventDtos(events);
            shortEventDtos.sort(Comparator.comparing(EventShortDto::getViews));
            if (shortEventDtos.size() > from) {
                if (shortEventDtos.size() > from + size) {
                    shortEventDtos = shortEventDtos.subList(from, from + size);
                } else {
                    shortEventDtos = shortEventDtos.subList(from, shortEventDtos.size());
                }
            } else {
                shortEventDtos = Collections.emptyList();
            }
            return shortEventDtos;
        }
        return createShortEventDtos(events);
    }

    List<EventShortDto> createShortEventDtos(List<Event> events) {
        HashMap<Long, Integer> eventIdsWithViewsCounter = new HashMap<>();

        LocalDateTime lowLocalDateTime = events.get(0).getCreatedOn();
        List<String> uris = new ArrayList<>();
        for (Event event : events) {
            uris.add("/events/" + event.getId().toString());
            if (lowLocalDateTime.isAfter(event.getCreatedOn())) {
                lowLocalDateTime = event.getCreatedOn();
            }
        }
        List<StatsResponseDto> viewsCounter = getViewsCounter(uris, lowLocalDateTime.format(DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)));
        for (StatsResponseDto statsDto : viewsCounter) {
            String[] split = statsDto.getUri().split("/");
            eventIdsWithViewsCounter.put(Long.parseLong(split[2]), Math.toIntExact(statsDto.getHits()));
        }
        List<ParticipationRequest> requests = requestRepository.findByEventIds(new ArrayList<>(eventIdsWithViewsCounter.keySet()));
        return events.stream()
                .map(EventDtoMapper::mapEventToShortDto)
                .peek(dto -> dto.setConfirmedRequests(
                        requests.stream()
                                .filter((request -> request.getEvent().getId().equals(dto.getId())))
                                .count()
                ))
                .peek(dto -> dto.setViews(eventIdsWithViewsCounter.get(dto.getId())))
                .collect(Collectors.toList());
    }

    EventFullDto getViewsCounter(EventFullDto eventFullDto) {
        Integer views = statsClient.getStats(eventFullDto.getCreatedOn(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)),
                List.of("/events/" + eventFullDto.getId()), true).size();
        eventFullDto.setViews(views);
        return eventFullDto;
    }

    List<StatsResponseDto> getViewsCounter(List<String> uris, String CreatedOn) {
        List<StatsResponseDto> viewsForUris = statsClient.getStats(CreatedOn,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)),
                uris, true);
        return viewsForUris;
    }

    List<EventFullDto> getViewCounters(List<EventFullDto> dtos) {
        if (dtos.size() > 0) {
            HashMap<Long, Integer> eventIdsWithViewsCounter = new HashMap<>();
            LocalDateTime lowLocalDateTime = LocalDateTime.parse(dtos.get(0).getCreatedOn().replace(" ", "T"));
            List<String> uris = new ArrayList<>();
            for (EventFullDto dto : dtos) {
                eventIdsWithViewsCounter.put(dto.getId(), 0);
                uris.add("/events/" + dto.getId().toString());
                if (lowLocalDateTime.isAfter(LocalDateTime.parse(dto.getCreatedOn().replace(" ", "T")))) {
                    lowLocalDateTime = LocalDateTime.parse(dto.getCreatedOn().replace(" ", "T"));
                }
            }
            List<StatsResponseDto> viewsCounter = getViewsCounter(uris, lowLocalDateTime.format(DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)));
            for (StatsResponseDto statsDto : viewsCounter) {
                String[] split = statsDto.getUri().split("/");
                eventIdsWithViewsCounter.put(Long.parseLong(split[2]), Math.toIntExact(statsDto.getHits()));
            }
            ArrayList<Long> longs = new ArrayList<>(eventIdsWithViewsCounter.keySet());
            List<ParticipationRequest> requests = requestRepository.findByEventIdsAndStatus(longs, "CONFIRMED");
            return dtos.stream()
                    .peek(dto -> dto.setConfirmedRequests(
                            requests.stream()
                                    .filter((request -> request.getEvent().getId().equals(dto.getId())))
                                    .count()
                    ))
                    .peek(dto -> dto.setViews(eventIdsWithViewsCounter.get(dto.getId())))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    EventFullDto getEventFullDto(Event event) {
        Long confirmed = requestRepository.countByEventAndStatuses(event.getId(), List.of("CONFIRMED"));
        return EventDtoMapper.mapEventToFullDto(event, confirmed);
    }

    Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие " + eventId + " не найдено"));
    }

    Event updateEventWithUserRequest(Event event, UpdateEventUserRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory()).orElseThrow(
                    () -> new NotFoundException(CATEGORY_NOT_FOUND + updateRequest.getCategory()));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(LocalDateTime.parse(updateRequest.getEventDate(), DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)));
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction().toUpperCase()) {
                case "PUBLISH_EVENT":
                    event.setState(EventState.PUBLISHED);
                    break;
                case "REJECT_EVENT":
                case "CANCEL_REVIEW":
                    event.setState(EventState.CANCELED);
                    break;
                case "SEND_TO_REVIEW":
                    event.setState(EventState.PENDING);
                    break;
                default:
                    throw new WrongDataException("Неверное состояние события, не удалось обновить");
            }
        }
        if (LocalDateTime.now().isAfter(event.getEventDate().minus(2, ChronoUnit.HOURS))) {
            throw new WrongDataException("До начала события осталось меньше 2 часов " + event.getId());
        }
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя изменить опубликованное событие " + event.getId());
        }
        return event;
    }

    Event updateEventWithAdminRequest(Event event, UpdateEventAdminRequest updateRequest) {
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getCategory() != null) {
            Category category = categoryRepository.findById(updateRequest.getCategory()).orElseThrow(
                    () -> new NotFoundException(CATEGORY_NOT_FOUND + updateRequest.getCategory()));
            event.setCategory(category);
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            event.setEventDate(LocalDateTime.parse(updateRequest.getEventDate(), DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)));
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getStateAction() != null) {
            switch (updateRequest.getStateAction().toUpperCase()) {
                case "PUBLISH_EVENT":
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                    break;
                case "REJECT_EVENT":
                    event.setState(EventState.CANCELED);
                    break;
                default:
                    throw new WrongDataException("Неверное состояние события, не удалось обновить");
            }
        }
        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        return event;
    }

    void saveLocation(Event event) {
        event.setLocation(locationRepository.save(event.getLocation()));
        log.info("Локация сохранена {}", event.getLocation().getId());
    }


}
