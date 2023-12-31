package ru.practicum.comments.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.comments.model.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findCommentsByEventIdAndStateOrderByCreatedDesc(Long eventId, String state, Pageable page);

    List<Comment> findCommentsByAuthorIdOrderByCreatedDesc(Long userId, Pageable page);
}
