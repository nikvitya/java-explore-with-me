package ru.practicum.event.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Location;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

import static ru.practicum.util.JsonFormatPattern.JSON_FORMAT_PATTERN_FOR_TIME;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {

    Long category;

    @NotBlank
    @Size(min = 20, max = 2000)
    String annotation;

    @Size(min = 3, max = 120)
    String title;

    @NotBlank
    @Size(min = 20)
    String description;

    @JsonFormat(pattern = JSON_FORMAT_PATTERN_FOR_TIME)
    LocalDateTime eventDate;

    Location location;
    Boolean paid;
    Integer participantLimit;
    Boolean requestModeration;
}
