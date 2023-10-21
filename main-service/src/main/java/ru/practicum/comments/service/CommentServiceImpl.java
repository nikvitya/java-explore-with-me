package ru.practicum.comments.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.dto.CommentDtoMapper;
import ru.practicum.comments.model.Comment;
import ru.practicum.comments.repository.CommentRepository;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.WrongDataException;
import ru.practicum.request.repository.RequestRepository;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static ru.practicum.comments.model.CommentStatus.APPROVED;
import static ru.practicum.comments.model.CommentStatus.REJECTED;


@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentServiceImpl implements CommentService {
    final EventRepository eventRepository;
    final UserRepository userRepository;
    final CommentRepository commentRepository;
    final RequestRepository requestRepository;

    @Override
    public CommentDto add(Long userId, Long eventId, CommentDto newCommentDto) {
        log.info("Создание нового комментария пользователем {} к событию {}", userId, eventId);
        User user = userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id= {} не найден", userId)
        );

        Event event = eventRepository.findById(eventId).orElseThrow(
                () -> new NotFoundException("Cобытие с id= {} не найдено", eventId)
        );

        Comment comment;

        Long count = requestRepository.countByPublishedEventsAndStatuses(
                userId,
                eventId,
                newCommentDto.getCreated() == null ? LocalDateTime.now() : newCommentDto.getCreated(),
                EventState.PUBLISHED,
                List.of("CONFIRMED", "ACCEPTED")
        );

        if (count > 0) {
            comment = commentRepository.save(getCommentFromDto(user, event, newCommentDto));
            log.info("Комментарий сохранен c id= {}", comment.getId());
        } else {
            throw new NotFoundException("Пользователь не был на событии, комментарий невозможно добавить.");
        }

        return CommentDtoMapper.mapCommentToDto(comment);
    }

    @Override
    public CommentDto getCommentById(Long commentId) {
        log.info("Получение комментария {}", commentId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий с id= {} не найден ", commentId)
        );
        return CommentDtoMapper.mapCommentToDto(comment);
    }

    @Override
    public List<CommentDto> getByEvent(Long eventId, Pageable page) {
        log.info("Получение комментариев к событию {}", eventId);
        List<Comment> comments = commentRepository.findCommentsByEventIdAndStateOrderByCreatedDesc(eventId, APPROVED, page);
        log.info("Найдено комментариев: " + comments.size());

        return comments.stream()
                .map(CommentDtoMapper::mapCommentToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getByUser(Long userId, Pageable page) {
        log.info("Получение комментариев пользователя {}", userId);
        List<Comment> comments = commentRepository.findCommentsByAuthorIdOrderByCreatedDesc(userId, page);
        log.info("Найдено комментариев: " + comments.size());

        return comments.stream()
                .map(CommentDtoMapper::mapCommentToDto)
                .collect(Collectors.toList());
    }

    @Override
    public CommentDto update(Long userId, Long commentId, CommentDto updateCommentDto) {
        log.info("Изменение комментария c id = {} пользователем id= {}", commentId, userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь c id= {} не найден", userId);
        }

        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий c id= {} не найден ", commentId));

        if (userId.equals(comment.getAuthor().getId())) {
            comment.setText(updateCommentDto.getText());
            comment.setUpdated(LocalDateTime.now());
            comment.setState("UPDATED");
            commentRepository.save(comment);
        } else {
            throw new WrongDataException("Пользователь c id=" + userId + " не является автором комментария. Изменение невозможно");
        }

        log.info("Комментарий c id = {} обновлен", commentId);

        return CommentDtoMapper.mapCommentToDto(comment);
    }

    @Override
    public void deleteByUser(Long userId, Long commentId) {
        log.info("Удаление комментария c id = {} пользователем id= {}", commentId, userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь c id= {} не найден", userId);
        }
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий c id= {} не найден ", commentId)
        );

        if (userId.equals(comment.getAuthor().getId())) {
            commentRepository.deleteById(commentId);
        } else {
            throw new WrongDataException("Пользователь c id=" + userId + " не является автором комментария. Удаление невозможно");
        }
        log.info("Комментарий c id= {} удален пользователем", commentId);

    }

    @Override
    public void deleteByAdmin(Long commentId) {
        log.info("Удаление комментария администратором {}", commentId);

        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Комментарий c id = {} не найден ", commentId);
        }
        commentRepository.deleteById(commentId);
        log.info("Комментарий с id= {} удален администратором", commentId);
    }

    @Override
    public CommentDto approveComment(Long commentId) {
        log.info("Утверждение комментария c id= {}", commentId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий c id= {} не найден ", commentId)
        );

        comment.setState(APPROVED);
        comment.setPublished(LocalDateTime.now());

        return CommentDtoMapper.mapCommentToDto(commentRepository.save(comment));
    }

    @Override
    public CommentDto rejectComment(Long commentId) {
        log.info("Отклонение комментария c id = {}", commentId);
        Comment comment = commentRepository.findById(commentId).orElseThrow(
                () -> new NotFoundException("Комментарий с id= {} не найден ", commentId)
        );
        comment.setState(REJECTED);
        return CommentDtoMapper.mapCommentToDto(commentRepository.save(comment));
    }

    private Comment getCommentFromDto(User user, Event event, CommentDto commentDto) {
        Comment comment = CommentDtoMapper.mapDtoToComment(commentDto);
        comment.setAuthor(user);
        comment.setEvent(event);
        return comment;
    }
}
