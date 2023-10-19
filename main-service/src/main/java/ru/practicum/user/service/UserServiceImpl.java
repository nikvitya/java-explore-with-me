package ru.practicum.user.service;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserDtoMapper;
import ru.practicum.user.model.User;
import ru.practicum.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserServiceImpl implements UserService {

    final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto add(NewUserRequest newUserDto) {
        log.info("Создание пользователя.....");
        if (userRepository.countByName(newUserDto.getName()) > 0) {
            throw new ConflictException("Пользователь уже существует");
        }
        User user = UserDtoMapper.mapNewUserRequestToUser(newUserDto);
        User savedUser = userRepository.save(user);
        return UserDtoMapper.mapUserToDto(savedUser);
    }

    @Override
    public List<UserDto> get(List<Long> ids, Integer from, Integer size) {
        log.info("Получение информации о пользователях");
        List<UserDto> userDtos = new ArrayList<>();
        Pageable pageable = PageRequest.of(from / size, size);

        if (ids == null) {
            List<User> users = userRepository.findAllPageable(pageable);
            for (User user : users) {
                userDtos.add(UserDtoMapper.mapUserToDto(user));
            }
        } else {
            List<User> users = userRepository.findAllByIdsPageable(ids, pageable);
            for (User user : users) {
                userDtos.add(UserDtoMapper.mapUserToDto(user));
            }
        }

        return userDtos;
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        log.info("Удаление пользователя....id={}", userId);
        userRepository.deleteById(userId);
        log.info("Пользователь удалён");
    }


}
