package ru.practicum.comments.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.event.model.Event;
import ru.practicum.user.model.User;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "text", nullable = false, length = 5000)
    String text;

    String state;

    @Column(nullable = false)
    LocalDateTime created;

    LocalDateTime published;

    LocalDateTime updated;

    @ManyToOne(fetch = FetchType.LAZY)
    User author;

    @ManyToOne(fetch = FetchType.LAZY)
    Event event;
}
