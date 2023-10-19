package ru.practicum.request.dto;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class EventRequestStatusUpdateResultMapper {

    public static EventRequestStatusUpdateResult mapToEventRequestStatusUpdateResult(List<ParticipationRequestDto> confirmedRequests,
                                                                                     List<ParticipationRequestDto> rejectedRequests) {
        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        result.setConfirmedRequests(confirmedRequests);
        result.setRejectedRequests(rejectedRequests);
        return result;
    }
}
