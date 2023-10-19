package ru.practicum.compilation.dto;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.compilation.model.Compilation;
import ru.practicum.event.model.Event;

import java.util.List;

@Component
@AllArgsConstructor
public class CompilationDtoMapper {

    public Compilation mapNewCompilationDtoToCompilation(NewCompilationDto dto, List<Event> events) {
        Compilation compilation = new Compilation();
        compilation.setEvents(events);
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned());
        return compilation;
    }

    public CompilationDto mapCompilationToDto(Compilation compilation) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }
}
