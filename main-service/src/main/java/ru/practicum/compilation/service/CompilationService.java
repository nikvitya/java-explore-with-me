package ru.practicum.compilation.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto add(NewCompilationDto compilationDto);

    CompilationDto update(Long compId, UpdateCompilationRequest compilationDto);

    List<CompilationDto> getAll(Boolean pinned, Pageable page);

    CompilationDto getCompilationById(Long compId);

    void delete(Long compId);
}
