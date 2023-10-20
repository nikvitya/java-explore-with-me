package ru.practicum.category.service;

import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.NewCategoryRequestDto;

import java.util.List;

public interface CategoryService {
    CategoryDto add(NewCategoryRequestDto newCategoryRequestDto);

    CategoryDto update(Long catId, CategoryDto categoryDto);

    CategoryDto getCategoryById(Long catId);

    List<CategoryDto> getAll(Integer from, Integer size);

    void delete(Long catId);
}
