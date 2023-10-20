package ru.practicum.request.dto;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.request.model.ParticipationRequest;

@Component
@AllArgsConstructor
public class RequestDtoMapper {
    public ParticipationRequestDto mapRequestToDto(ParticipationRequest participationRequest) {
        ParticipationRequestDto dto = new ParticipationRequestDto();
        dto.setId(participationRequest.getId());
        dto.setRequester(participationRequest.getRequester().getId());
        dto.setEvent(participationRequest.getEvent().getId());
        dto.setStatus(participationRequest.getStatus());
        dto.setCreated(participationRequest.getCreated());
        return dto;
    }
}
