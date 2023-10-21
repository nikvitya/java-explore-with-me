package ru.practicum.request.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.WrongDataException;
import ru.practicum.request.dto.*;
import ru.practicum.request.model.ParticipationRequest;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.request.model.RequestStatus.*;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestServiceImpl implements RequestService {

    final EventRepository eventRepository;
    final RequestDtoMapper requestDtoMapper;
    final UserService userService;
    final RequestRepository requestRepository;

    @Override
    public List<ParticipationRequestDto> getParticipationRequestsDto(Long userId, Long eventId) {
        log.info("Получение информации о заявках пользователя {} на участие в событиях", userId);
        List<ParticipationRequest> requests = getParticipationRequests(userId, eventId);
        return requests.stream()
                .map(requestDtoMapper::mapRequestToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult update(Long userId,
                                                 Long eventId,
                                                 EventRequestStatusUpdateRequest updateRequest) {
        log.info("Изменение статуса заявок на участие в событии текущего пользователя с id= {}", userId);

        Event event = getEventById(eventId);
        List<ParticipationRequest> requests = getParticipationRequestsByEventId(eventId);
        Long confirmedRequestsCounter = requests.stream().filter(r -> r.getStatus().equals(CONFIRMED_REQUEST)).count();

        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        List<ParticipationRequest> result = new ArrayList<>();

        List<ParticipationRequest> pending = requests.stream()
                .filter(p -> p.getStatus().equals(PENDING_REQUEST)).collect(Collectors.toList());


        for (ParticipationRequest request : requests) {
            if (request.getStatus().equals(CONFIRMED_REQUEST) || request.getStatus().equals(REJECTED_REQUEST) || request.getStatus().equals(PENDING_REQUEST)) {

                if (updateRequest.getStatus().equals(CONFIRMED_REQUEST) && event.getParticipantLimit() != 0) {
                    if (event.getParticipantLimit() < confirmedRequestsCounter) {

                        pending.stream().peek(p -> p.setStatus(REJECTED_REQUEST)).collect(Collectors.toList());

                        log.info("Слишком много заявок на участие");
                        throw new ConflictException("Слишком много заявок на участие");
                    }
                }

                if (updateRequest.getStatus().equals(REJECTED_REQUEST) && request.getStatus().equals(CONFIRMED_REQUEST)) {
                    throw new ConflictException("Нельзя отменять подтверждённую заявку через отсюда");
                }

                request.setStatus(updateRequest.getStatus());
                ParticipationRequestDto participationRequestDto = requestDtoMapper.mapRequestToDto(request);

                if ("CONFIRMED".equals(participationRequestDto.getStatus())) {
                    confirmedRequests.add(participationRequestDto);
                } else if ("REJECTED".equals(participationRequestDto.getStatus())) {
                    rejectedRequests.add(participationRequestDto);
                }

                result.add(request);
                confirmedRequestsCounter++;

            } else {
                throw new WrongDataException("Неверный статус");
            }
        }

        requestRepository.saveAll(pending);

        requestRepository.saveAll(result);

        return EventRequestStatusUpdateResultMapper.mapToEventRequestStatusUpdateResult(confirmedRequests, rejectedRequests);
    }

    @Override
    public List<ParticipationRequestDto> getParticipationRequestsByUserId(Long userId) {
        log.info("Получение информации о заявках пользователя..");
        if (!userService.isUserExistById(userId)){
            throw new NotFoundException("Пользователь с id={} не найден", userId);
        }
        return requestRepository.findByUserId(userId).stream()
                .map(requestDtoMapper::mapRequestToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto add(Long userId, Long eventId) {
        log.info("Заявка пользователем {} запроса на участие в событии {}", userId, eventId);

        User user = userService.getUserById(userId);
        Event event = getEventById(eventId);

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Владелец не может подать заявку на участие");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Событие не опубликовано, заявка не подана");
        }
        List<ParticipationRequest> requests = getParticipationRequestsByEventId(event.getId());
        if (participationLimitIsFull(event)) {
            throw new ConflictException("Достигнут лимит заявок, заявка не подана");
        }
        for (ParticipationRequest request : requests) {
            if (request.getRequester().getId().equals(userId)) {
                throw new ConflictException("Оставить заявку повторно невозможно");
            }
        }

        ParticipationRequest newRequest = createNewParticipationRequest(user, event);
        return requestDtoMapper.mapRequestToDto(requestRepository.save(newRequest));
    }

    private ParticipationRequest createNewParticipationRequest(User user, Event event) {
        ParticipationRequest newRequest = new ParticipationRequest();
        newRequest.setRequester(user);
        newRequest.setCreated(LocalDateTime.now().toString());
        if (event.getParticipantLimit() == 0) {
            newRequest.setStatus(CONFIRMED_REQUEST);
        } else {
            newRequest.setStatus(PENDING_REQUEST);
        }
        newRequest.setEvent(event);
        if (!event.getRequestModeration()) {
            newRequest.setStatus(ACCEPTED_REQUEST);
        }
        return newRequest;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancel(Long userId, Long requestId) {
        log.info("Отмена пользователем {} своего запроса на участие в {}", userId, requestId);

        if (!userService.isUserExistById(userId)) {
            throw new NotFoundException("Пользователь с id={} не найден", userId);
        }

        ParticipationRequest request = requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Запрос не существует")
        );
        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Заявка  оставлена не пользователем " + userId);
        }
        request.setStatus(CANCELED_REQUEST);
        log.info("Отмена заявки на участие " + requestId);
        return requestDtoMapper.mapRequestToDto(requestRepository.save(request));
    }


    private boolean participationLimitIsFull(Event event) {
        Long confirmedRequestsCounter = requestRepository.countByEventAndStatuses(event.getId(), List.of("CONFIRMED", "ACCEPTED"));
        if (event.getParticipantLimit() != 0 && event.getParticipantLimit() <= confirmedRequestsCounter) {
            throw new ConflictException("Слишком много заявок на участие");
        }
        return false;
    }

    private List<ParticipationRequest> getParticipationRequests(Long userId, Long eventId) {
        User user = userService.getUserById(userId);
        Event event = getEventById(eventId);
        if (!user.getId().equals(event.getInitiator().getId())) {
            throw new WrongDataException("Пользователь не инициатор события " + eventId);
        }
        return requestRepository.findByEventInitiatorId(userId);
    }

    private List<ParticipationRequest> getParticipationRequestsByEventId(Long eventId) {
        if (eventRepository.existsById(eventId)) {
            return requestRepository.findByEventId(eventId);
        } else {
            throw new NotFoundException("Событие не найдено");
        }
    }

    private Event getEventById(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Событие не найдено"));
    }


}
