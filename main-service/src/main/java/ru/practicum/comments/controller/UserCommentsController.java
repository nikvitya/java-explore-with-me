package ru.practicum.comments.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.comments.dto.CommentDto;
import ru.practicum.comments.service.CommentService;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCommentsController {

    final CommentService commentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto add(@PathVariable Long userId,
                          @RequestParam Long eventId,
                          @Valid @RequestBody CommentDto newCommentDto) {
        return commentService.add(userId, eventId, newCommentDto);
    }

    @GetMapping
    public List<CommentDto> getByUser(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "0") Integer from,
                                      @RequestParam(defaultValue = "10") Integer size) {
        return commentService.getByUser(userId, PageRequest.of(from / size, size));
    }

    @PatchMapping("/{commentId}")
    public CommentDto update(@PathVariable Long userId,
                             @PathVariable Long commentId,
                             @Valid @RequestBody CommentDto updateCommentDto) {
        return commentService.update(userId, commentId, updateCommentDto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long userId,
                       @PathVariable Long commentId) {
        commentService.deleteByUser(userId, commentId);
    }
}
