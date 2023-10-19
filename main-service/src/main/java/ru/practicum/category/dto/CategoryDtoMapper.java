package ru.practicum.category.dto;

import lombok.experimental.UtilityClass;
import ru.practicum.category.model.Category;

@UtilityClass
public class CategoryDtoMapper {
    public static Category mapNewDtoToCategory(NewCategoryRequestDto newCategoryRequestDto) {
        return new Category(null, newCategoryRequestDto.getName());
    }

    public static CategoryDto mapCategoryToDto(Category category) {
        return new CategoryDto(category.getId(), category.getName());
    }

}
