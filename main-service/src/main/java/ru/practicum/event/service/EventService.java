package ru.practicum.event.service;

import ru.practicum.event.dto.*;

import java.util.List;

public interface EventService {

    EventFullDto add(Long userId, NewEventDto newEvent);

    EventFullDto update(Long eventId, UpdateEventAdminRequest updateRequest);

    EventFullDto updateEventOfUserByIds(Long userId, Long eventId, UpdateEventUserRequest request);

    List<EventFullDto> get(List<Long> users, List<String> states, List<Long> categories, String rangeStart, String rangeEnd, Integer from, Integer size);

    List<EventShortDto> getEventsByUser(Long userId, Integer from, Integer size);

    EventFullDto getEventDtoById(Long eventId, String uri, String ip);

    EventFullDto getEventOfUserByIds(Long userId, Long eventId);

    List<EventShortDto> getEventsWithFilters(String text,
                                             List<Long> categories,
                                             Boolean paid,
                                             String rangeStart,
                                             String rangeEnd,
                                             Boolean onlyAvailable,
                                             String sort,
                                             Integer from,
                                             Integer size,
                                             String uri,
                                             String ip);
}
