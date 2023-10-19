package ru.practicum.compilation.service;

import ru.practicum.compilation.dto.CompilationDto;
import ru.practicum.compilation.dto.NewCompilationDto;
import ru.practicum.compilation.dto.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    CompilationDto add(NewCompilationDto compilationDto);

    CompilationDto update(Long compId, UpdateCompilationRequest compilationDto);

    List<CompilationDto> getAll(String pinned, Integer from, Integer size);

    CompilationDto getCompilationById(Long compId);

    void delete(Long compId);
}
