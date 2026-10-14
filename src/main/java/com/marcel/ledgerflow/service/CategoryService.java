package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.CategoryResponse;
import com.marcel.ledgerflow.dto.CreateCategoryRequest;
import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.CategoryRepository;
import com.marcel.ledgerflow.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.marcel.ledgerflow.exception.DuplicateCategoryException;
import java.util.List;
import com.marcel.ledgerflow.exception.CategoryNotFoundException;
import com.marcel.ledgerflow.dto.UpdateCategoryRequest;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            UserRepository userRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CategoryResponse createCategory(
            Long userId,
            CreateCategoryRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found"
                        )
                );

        String categoryName = request.name().trim();

        boolean categoryExists =
                categoryRepository.existsByUserIdAndNameAndType(
                        userId,
                        categoryName,
                        request.type()
                );

        if (categoryExists) {
            throw new DuplicateCategoryException();
        }

        Category category = new Category();
        category.setUser(user);
        category.setName(categoryName);
        category.setType(request.type());

        Category savedCategory =
                categoryRepository.saveAndFlush(category);

        return new CategoryResponse(
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getType(),
                savedCategory.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoriesByUserId(Long userId) {

        return categoryRepository
                .findAllByUserIdOrderByNameAsc(userId)
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getName(),
                        category.getType(),
                        category.getCreatedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long categoryId, Long userId) {

        Category category = categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getCreatedAt()
        );
    }

    @Transactional
    public CategoryResponse updateCategory(
            Long categoryId,
            Long userId,
            UpdateCategoryRequest request
    ) {

        // Find the category and verify ownership
        Category category = categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);

        String categoryName = request.name().trim();

        // Check whether the name or type is changing
        boolean categoryChanged =
                !category.getName().equals(categoryName)
                        || category.getType() != request.type();

        // Prevent duplicate categories
        if (categoryChanged) {

            boolean categoryExists =
                    categoryRepository.existsByUserIdAndNameAndType(
                            userId,
                            categoryName,
                            request.type()
                    );

            if (categoryExists) {
                throw new DuplicateCategoryException();
            }
        }

        // Update the category
        category.setName(categoryName);
        category.setType(request.type());

        Category savedCategory = categoryRepository.saveAndFlush(category);

        return new CategoryResponse(
                savedCategory.getId(),
                savedCategory.getName(),
                savedCategory.getType(),
                savedCategory.getCreatedAt()
        );
    }

    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {

        // Find the category and verify ownership
        Category category = categoryRepository
                .findByIdAndUserId(categoryId, userId)
                .orElseThrow(CategoryNotFoundException::new);

        // Delete the category
        categoryRepository.delete(category);
    }
}