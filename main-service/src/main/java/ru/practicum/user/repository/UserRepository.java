package ru.practicum.user.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.user.model.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    long countByName(String name);

    @Query("SELECT u FROM User u " +
            "WHERE(:ids IS NULL OR u.id IN :ids)")
    List<User> findAllByIdsPageable(List<Long> ids, Pageable page);

    @Query("SELECT u FROM User u " +
            "WHERE u.name = :name")
    List<User> findByName(String name);
}