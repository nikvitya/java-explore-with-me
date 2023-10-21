package ru.practicum.comments.dto;

import lombok.experimental.UtilityClass;
import ru.practicum.comments.model.Comment;
import ru.practicum.event.dto.EventDtoMapper;
import ru.practicum.user.dto.UserDtoMapper;

@UtilityClass
public class CommentDtoMapper {

    public static CommentDto mapCommentToDto(Comment comment) {
        CommentDto commentDto = new CommentDto();
        commentDto.setId(comment.getId());
        commentDto.setText(comment.getText());
        commentDto.setEvent(EventDtoMapper.mapEventToShortDto(comment.getEvent()));
        commentDto.setAuthor(UserDtoMapper.mapUserToShortDto(comment.getAuthor()));
        commentDto.setCreated(comment.getCreated());
        commentDto.setState(comment.getState());
        commentDto.setPublished(comment.getPublished());
        commentDto.setUpdated(comment.getUpdated());
        return commentDto;
    }

    public static Comment mapDtoToComment(CommentDto dto) {
        Comment comment = new Comment();
        comment.setText(dto.getText());
        comment.setCreated(dto.getCreated());

        return comment;
    }
}
