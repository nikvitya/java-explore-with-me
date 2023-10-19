package ru.practicum.user.dto;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.user.model.User;

@Component
@AllArgsConstructor
public class UserDtoMapper {
    public static UserDto mapUserToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }

    public static UserShortDto mapUserToShortDto(User user) {
        if (user.getId() == null) {
            return new UserShortDto(null, user.getName());
        }
        return new UserShortDto(user.getId(), user.getName());
    }

    public static User mapNewUserRequestToUser(NewUserRequest newUser) {
        return new User(
                null,
                newUser.getEmail(),
                newUser.getName()
        );
    }

    public User mapDtoToUser(UserDto userDto) {
        return new User(
                userDto.getId(),
                userDto.getEmail(),
                userDto.getName()
        );
    }
}
