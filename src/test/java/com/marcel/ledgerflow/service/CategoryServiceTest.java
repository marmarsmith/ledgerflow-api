package com.marcel.ledgerflow.service;

import com.marcel.ledgerflow.dto.CategoryResponse;
import com.marcel.ledgerflow.dto.CreateCategoryRequest;
import com.marcel.ledgerflow.entity.Category;
import com.marcel.ledgerflow.enums.TransactionType;
import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.exception.DuplicateCategoryException;
import com.marcel.ledgerflow.repository.CategoryRepository;
import com.marcel.ledgerflow.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;

import com.marcel.ledgerflow.exception.CategoryNotFoundException;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.marcel.ledgerflow.dto.UpdateCategoryRequest;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategoryForAuthenticatedUser() {

        // Arrange
        Long userId = 5L;

        CreateCategoryRequest request = new CreateCategoryRequest(
                "  Groceries  ",
                TransactionType.EXPENSE
        );

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.existsByUserIdAndNameAndType(
                userId,
                "Groceries",
                TransactionType.EXPENSE
        )).thenReturn(false);

        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(10L);
                    category.setCreatedAt(
                            OffsetDateTime.parse("2026-10-14T12:00:00Z")
                    );
                    return category;
                });

        // Act
        CategoryResponse response =
                categoryService.createCategory(userId, request);

        // Assert
        assertEquals(10L, response.id());
        assertEquals("Groceries", response.name());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertNotNull(response.createdAt());

        verify(userRepository).findById(userId);

        verify(categoryRepository).existsByUserIdAndNameAndType(
                userId,
                "Groceries",
                TransactionType.EXPENSE
        );

        verify(categoryRepository).saveAndFlush(argThat(category ->
                category.getUser().getId().equals(userId)
                        && category.getName().equals("Groceries")
                        && category.getType() == TransactionType.EXPENSE
        ));
    }

    @Test
    void shouldNotCreateDuplicateCategory() {

        // Arrange
        Long userId = 5L;

        CreateCategoryRequest request = new CreateCategoryRequest(
                "  Groceries  ",
                TransactionType.EXPENSE
        );

        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(categoryRepository.existsByUserIdAndNameAndType(
                userId,
                "Groceries",
                TransactionType.EXPENSE
        )).thenReturn(true);

        // Act & Assert
        DuplicateCategoryException exception = assertThrows(
                DuplicateCategoryException.class,
                () -> categoryService.createCategory(userId, request)
        );

        assertEquals(
                "Category already exists for this transaction type",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .saveAndFlush(any(Category.class));
    }

    @Test
    void shouldNotCreateCategoryWhenUserDoesNotExist() {

        // Arrange
        Long userId = 999L;

        CreateCategoryRequest request = new CreateCategoryRequest(
                "Groceries",
                TransactionType.EXPENSE
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.createCategory(userId, request)
        );

        assertEquals(
                "Authenticated user not found",
                exception.getMessage()
        );

        verify(userRepository).findById(userId);

        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldReturnCategoriesForAuthenticatedUser() {

        // Arrange
        Long userId = 5L;

        User user = new User();
        user.setId(userId);

        Category entertainment = new Category();
        entertainment.setId(10L);
        entertainment.setUser(user);
        entertainment.setName("Entertainment");
        entertainment.setType(TransactionType.EXPENSE);
        entertainment.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        Category groceries = new Category();
        groceries.setId(11L);
        groceries.setUser(user);
        groceries.setName("Groceries");
        groceries.setType(TransactionType.EXPENSE);
        groceries.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T12:05:00Z")
        );

        when(categoryRepository.findAllByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(entertainment, groceries));

        // Act
        List<CategoryResponse> responses =
                categoryService.getCategoriesByUserId(userId);

        // Assert
        assertEquals(2, responses.size());

        assertEquals(10L, responses.get(0).id());
        assertEquals("Entertainment", responses.get(0).name());
        assertEquals(TransactionType.EXPENSE, responses.get(0).type());

        assertEquals(11L, responses.get(1).id());
        assertEquals("Groceries", responses.get(1).name());
        assertEquals(TransactionType.EXPENSE, responses.get(1).type());

        verify(categoryRepository)
                .findAllByUserIdOrderByNameAsc(userId);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoCategories() {

        // Arrange
        Long userId = 5L;

        when(categoryRepository.findAllByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of());

        // Act
        List<CategoryResponse> responses =
                categoryService.getCategoriesByUserId(userId);

        // Assert
        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(categoryRepository)
                .findAllByUserIdOrderByNameAsc(userId);
    }

    @Test
    void shouldReturnCategoryWhenItBelongsToAuthenticatedUser() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);
        category.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act
        CategoryResponse response =
                categoryService.getCategoryById(categoryId, userId);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Groceries", response.name());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(category.getCreatedAt(), response.createdAt());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);
    }

    @Test
    void shouldNotReturnCategoryWhenItBelongsToAnotherUser() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 8L;

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.getCategoryById(categoryId, userId)
        );

        assertEquals("Category not found", exception.getMessage());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);
    }

    @Test
    void shouldUpdateCategoryForAuthenticatedUser() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);
        category.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Food & Groceries",
                TransactionType.EXPENSE
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByUserIdAndNameAndType(
                userId,
                "Food & Groceries",
                TransactionType.EXPENSE
        )).thenReturn(false);

        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryResponse response =
                categoryService.updateCategory(categoryId, userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Food & Groceries", response.name());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(category.getCreatedAt(), response.createdAt());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(categoryRepository)
                .existsByUserIdAndNameAndType(
                        userId,
                        "Food & Groceries",
                        TransactionType.EXPENSE
                );

        verify(categoryRepository).saveAndFlush(category);
    }

    @Test
    void shouldNotUpdateCategoryWhenItBelongsToAnotherUser() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 8L;

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Unauthorized Update",
                TransactionType.EXPENSE
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.updateCategory(
                        categoryId,
                        userId,
                        request
                )
        );

        assertEquals("Category not found", exception.getMessage());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(categoryRepository, never())
                .saveAndFlush(any(Category.class));

        verify(categoryRepository, never())
                .existsByUserIdAndNameAndType(
                        anyLong(),
                        anyString(),
                        any(TransactionType.class)
                );
    }

    @Test
    void shouldNotUpdateCategoryWhenDuplicateExists() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);
        category.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Entertainment",
                TransactionType.EXPENSE
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(categoryRepository.existsByUserIdAndNameAndType(
                userId,
                "Entertainment",
                TransactionType.EXPENSE
        )).thenReturn(true);

        // Act & Assert
        DuplicateCategoryException exception = assertThrows(
                DuplicateCategoryException.class,
                () -> categoryService.updateCategory(
                        categoryId,
                        userId,
                        request
                )
        );

        assertEquals(
                "Category already exists for this transaction type",
                exception.getMessage()
        );

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(categoryRepository)
                .existsByUserIdAndNameAndType(
                        userId,
                        "Entertainment",
                        TransactionType.EXPENSE
                );

        verify(categoryRepository, never())
                .saveAndFlush(any(Category.class));

        // Verify the original category remains unchanged
        assertEquals("Groceries", category.getName());
        assertEquals(TransactionType.EXPENSE, category.getType());
    }

    @Test
    void shouldUpdateCategoryWhenValuesAreUnchanged() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);
        category.setCreatedAt(
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Groceries",
                TransactionType.EXPENSE
        );

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CategoryResponse response =
                categoryService.updateCategory(categoryId, userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Groceries", response.name());
        assertEquals(TransactionType.EXPENSE, response.type());
        assertEquals(category.getCreatedAt(), response.createdAt());

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        // No duplicate check should occur
        verify(categoryRepository, never())
                .existsByUserIdAndNameAndType(
                        anyLong(),
                        anyString(),
                        any(TransactionType.class)
                );

        verify(categoryRepository).saveAndFlush(category);
    }

    @Test
    void shouldDeleteCategoryForAuthenticatedUser() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        User user = new User();
        user.setId(userId);

        Category category = new Category();
        category.setId(categoryId);
        category.setUser(user);
        category.setName("Groceries");
        category.setType(TransactionType.EXPENSE);

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.of(category));

        // Act
        categoryService.deleteCategory(categoryId, userId);

        // Assert
        verify(categoryRepository).findByIdAndUserId(categoryId, userId);
        verify(categoryRepository).delete(category);
    }

    @Test
    void shouldNotDeleteCategoryWhenItBelongsToAnotherUser() {

        // Arrange
        Long categoryId = 10L;
        Long userId = 8L;

        when(categoryRepository.findByIdAndUserId(categoryId, userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CategoryNotFoundException.class,
                () -> categoryService.deleteCategory(categoryId, userId)
        );

        verify(categoryRepository)
                .findByIdAndUserId(categoryId, userId);

        verify(categoryRepository, never())
                .delete(any(Category.class));
    }
}