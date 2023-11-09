package ru.practicum.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserService {

    UserDto add(NewUserRequest newUserDto);

    List<UserDto> get(List<Long> ids, Pageable page);

    User getUserById(Long userId);

    void delete(Long userId);

    Boolean isUserExistById(Long userId);
}
