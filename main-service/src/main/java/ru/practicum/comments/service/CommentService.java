package ru.practicum.comments.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.comments.dto.CommentDto;

import java.util.List;

public interface CommentService {

    CommentDto add(Long userId, Long eventId, CommentDto newCommentDto);

    CommentDto getCommentById(Long commentId);

    List<CommentDto> getByEvent(Long eventId, Pageable page);

    List<CommentDto> getByUser(Long userId, Pageable page);

    CommentDto update(Long userId, Long commentId, CommentDto updateCommentDto);

    void deleteByUser(Long userId, Long commentId);

    void deleteByAdmin(Long commentId);

    CommentDto approveComment(Long commentId);

    CommentDto rejectComment(Long commentId);
}
