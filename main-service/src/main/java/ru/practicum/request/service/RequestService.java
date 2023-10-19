package ru.practicum.request.service;

import ru.practicum.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getParticipationRequestsDto(Long userId, Long eventId);

    EventRequestStatusUpdateResult update(Long userId,
                                          Long eventId,
                                          EventRequestStatusUpdateRequest updateRequest);

    List<ParticipationRequestDto> getParticipationRequestsByUserId(Long userId);

    ParticipationRequestDto add(Long userId, Long eventId);

    ParticipationRequestDto cancel(Long userId, Long requestId);
}
