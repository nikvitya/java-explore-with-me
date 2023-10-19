package ru.practicum.category.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.dto.CategoryDto;
import ru.practicum.category.dto.CategoryDtoMapper;
import ru.practicum.category.dto.NewCategoryRequestDto;
import ru.practicum.category.model.Category;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.event.repository.EventRepository;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.practicum.util.ErrorMessages.*;
import static ru.practicum.util.LogMessages.*;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryServiceImpl implements CategoryService {

    final CategoryRepository categoryRepository;
    final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto add(NewCategoryRequestDto newCategoryRequestDto) {
        log.info(CATEGORY_SAVING);

        if (categoryRepository.existsByName(newCategoryRequestDto.getName())) {
            throw new ConflictException(CATEGORY_NAME_ALREADY_EXIST);
        }

        Category category = categoryRepository.save(CategoryDtoMapper.mapNewDtoToCategory(newCategoryRequestDto));
        log.info(CATEGORY_SAVED);

        return CategoryDtoMapper.mapCategoryToDto(category);
    }

    @Override
    @Transactional
    public CategoryDto update(Long catId, CategoryDto categoryDto) {
        log.info(String.format(CATEGORY_CHANGING + "%s", catId));
        Category category = categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND));

        List<Category> categoryList = categoryRepository.findByName(categoryDto.getName());

        Long id = null;
        if (!categoryList.isEmpty()) {
            for (Category c : categoryList) {
                if (Objects.equals(c.getId(), catId)) {
                    id = c.getId();
                    break;
                }
            }
            if (id == null) {
                throw new ConflictException(CATEGORY_NAME_ALREADY_EXIST);
            }
        }

        category.setName(categoryDto.getName());
        return CategoryDtoMapper.mapCategoryToDto(categoryRepository.save(category));
    }

    @Override
    public CategoryDto getCategoryById(Long catId) {
        log.info(String.format(CATEGORY_GETTING + "%s", catId));

        Category category = categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND));

        return CategoryDtoMapper.mapCategoryToDto(category);
    }

    @Override
    public List<CategoryDto> getAll(Integer from, Integer size) {
        log.info(CATEGORY_GETTING_ALL);

        return categoryRepository.findAll(PageRequest.of(from / size, size)).getContent()
                .stream().map(CategoryDtoMapper::mapCategoryToDto).collect(Collectors.toList());

        }

    @Override
    @Transactional
    public void delete(Long catId) {
        log.info(String.format(CATEGORY_DELETING + "%s", catId));

        Category category = categoryRepository.findById(catId).orElseThrow(() -> new NotFoundException(CATEGORY_NOT_FOUND));

        if (!eventRepository.existsByCategoryId(catId)) {
            categoryRepository.deleteById(catId);
            log.info(CATEGORY_DELETED);
        } else {
            throw new ConflictException(CATEGORY_CANNOT_BE_DELETED);
        }
    }

}
