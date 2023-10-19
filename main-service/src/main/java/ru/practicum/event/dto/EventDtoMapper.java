package ru.practicum.event.dto;

import lombok.experimental.UtilityClass;
import ru.practicum.category.dto.CategoryDtoMapper;
import ru.practicum.category.model.Category;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.user.dto.UserDtoMapper;

import java.time.format.DateTimeFormatter;

import static ru.practicum.util.JsonFormatPattern.JSON_FORMAT_PATTERN_FOR_TIME;

@UtilityClass
public class EventDtoMapper {

    public static EventFullDto mapEventToFullDto(Event event, Long confirmed) {
        if (event.getState() == null) {
            event.setState(EventState.PENDING);
        }
        EventFullDto eventFullDto = new EventFullDto();
        eventFullDto.setId(event.getId());
        eventFullDto.setAnnotation(event.getAnnotation());
        eventFullDto.setCategory(CategoryDtoMapper.mapCategoryToDto(event.getCategory()));
        eventFullDto.setConfirmedRequests(confirmed);
        eventFullDto.setCreatedOn(event.getCreatedOn().format(DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME)));
        eventFullDto.setDescription(event.getDescription());
        eventFullDto.setEventDate(event.getEventDate());
        eventFullDto.setInitiator(UserDtoMapper.mapUserToShortDto(event.getInitiator()));
        eventFullDto.setLocation(event.getLocation());
        eventFullDto.setPaid(event.getPaid());
        eventFullDto.setParticipantLimit(event.getParticipantLimit());
        eventFullDto.setPublishedOn(checkPublishedOn(event));
        eventFullDto.setRequestModeration(event.getRequestModeration());
        eventFullDto.setState(event.getState().toString());
        eventFullDto.setTitle(event.getTitle());
        return eventFullDto;
    }

    public static EventShortDto mapEventToShortDto(Event event) {
        EventShortDto eventShortDto = new EventShortDto();
        eventShortDto.setAnnotation(event.getAnnotation());
        eventShortDto.setTitle(event.getTitle());
        eventShortDto.setCategory(CategoryDtoMapper.mapCategoryToDto(event.getCategory()));
        eventShortDto.setInitiator(UserDtoMapper.mapUserToShortDto(event.getInitiator()));
        eventShortDto.setId(event.getId());
        eventShortDto.setPaid(event.getPaid());
        eventShortDto.setEventDate(event.getEventDate());
        return eventShortDto;
    }

    public static Event mapNewEventDtoToEvent(NewEventDto newEvent, Category category) {
        Event event = new Event();
        event.setAnnotation(newEvent.getAnnotation());
        event.setCategory(category);
        event.setDescription(newEvent.getDescription());
        event.setEventDate(newEvent.getEventDate());
        event.setLocation(newEvent.getLocation());
        event.setPaid(newEvent.getPaid());
        event.setParticipantLimit(newEvent.getParticipantLimit());
        event.setRequestModeration(newEvent.getRequestModeration());
        event.setTitle(newEvent.getTitle());
        return event;
    }

    static String checkPublishedOn(Event event) {
        if (event.getPublishedOn() == null) {
            return null;
        }
        return event.getPublishedOn().format(DateTimeFormatter.ofPattern(JSON_FORMAT_PATTERN_FOR_TIME));
    }


}
