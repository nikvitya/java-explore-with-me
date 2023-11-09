package ru.practicum.event.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.category.model.Category;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.user.model.User;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 2000)
    String annotation;

    @Column(length = 7000)
    String description;

    @Column(nullable = false)
    String title;

    EventState state;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinTable(name = "events_to_categories",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    Category category;

    @Column(name = "created_on")
    LocalDateTime createdOn;

    @Column(nullable = false, name = "event_date")
    LocalDateTime eventDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    User initiator;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    Location location;

    @Column
    Boolean paid;

    @Column(name = "participant_limit")
    Integer participantLimit;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @Column(name = "request_moderation")
    Boolean requestModeration;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "events_to_compilations",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id"))
    List<Compilation> compilationList;
}
