package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.event.model.Location;
import ru.practicum.user.dto.UserShortDto;

import java.time.LocalDateTime;

import static ru.practicum.util.JsonFormatPattern.JSON_FORMAT_PATTERN_FOR_TIME;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {

    Long id;
    String title;
    String annotation;
    String description;
    String state;

    CategoryDto category;
    Long confirmedRequests;

    String createdOn;
    String publishedOn;

    UserShortDto initiator;
    Location location;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = JSON_FORMAT_PATTERN_FOR_TIME)
    LocalDateTime eventDate;

    Boolean paid;
    Boolean requestModeration;

    Integer participantLimit;
    Integer views;

}
