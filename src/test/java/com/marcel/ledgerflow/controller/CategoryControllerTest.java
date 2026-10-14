package com.marcel.ledgerflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcel.ledgerflow.dto.CategoryResponse;
import com.marcel.ledgerflow.dto.CreateCategoryRequest;
import com.marcel.ledgerflow.entity.TransactionType;
import com.marcel.ledgerflow.exception.GlobalExceptionHandler;
import com.marcel.ledgerflow.service.CategoryService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.marcel.ledgerflow.exception.DuplicateCategoryException;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.marcel.ledgerflow.exception.CategoryNotFoundException;

import com.marcel.ledgerflow.dto.UpdateCategoryRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

class CategoryControllerTest {

    private MockMvc mockMvc;
    private CategoryService categoryService;
    private ObjectMapper objectMapper;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {

        categoryService = mock(CategoryService.class);

        CategoryController controller =
                new CategoryController(categoryService);

        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        validator.close();
    }

    @Test
    void shouldReturn201WhenCategoryIsCreated() throws Exception {

        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Groceries",
                TransactionType.EXPENSE
        );

        CategoryResponse response = new CategoryResponse(
                10L,
                "Groceries",
                TransactionType.EXPENSE,
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        when(categoryService.createCategory(
                eq(5L),
                any(CreateCategoryRequest.class)
        )).thenReturn(response);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(categoryService).createCategory(
                eq(5L),
                any(CreateCategoryRequest.class)
        );
    }

    @Test
    void shouldReturn400WhenCategoryNameIsBlank() throws Exception {

        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "",
                TransactionType.EXPENSE
        );

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldReturn400WhenCategoryTypeIsMissing() throws Exception {

        // Arrange
        CreateCategoryRequest request = new CreateCategoryRequest(
                "Groceries",
                null
        );

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldReturn409WhenCategoryAlreadyExists() throws Exception {

        // Arrange
        Long userId = 5L;

        CreateCategoryRequest request = new CreateCategoryRequest(
                "Groceries",
                TransactionType.EXPENSE
        );

        when(categoryService.createCategory(
                eq(userId),
                any(CreateCategoryRequest.class)
        )).thenThrow(new DuplicateCategoryException());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Category already exists for this transaction type"
                ));

        verify(categoryService).createCategory(
                eq(userId),
                any(CreateCategoryRequest.class)
        );
    }

    @Test
    void shouldReturnCategoriesForAuthenticatedUser() throws Exception {

        // Arrange
        Long userId = 5L;

        CategoryResponse entertainment = new CategoryResponse(
                10L,
                "Entertainment",
                TransactionType.EXPENSE,
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        CategoryResponse groceries = new CategoryResponse(
                11L,
                "Groceries",
                TransactionType.EXPENSE,
                OffsetDateTime.parse("2026-10-14T12:05:00Z")
        );

        when(categoryService.getCategoriesByUserId(userId))
                .thenReturn(List.of(entertainment, groceries));

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Entertainment"))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(jsonPath("$[1].id").value(11))
                .andExpect(jsonPath("$[1].name").value("Groceries"))
                .andExpect(jsonPath("$[1].type").value("EXPENSE"))
                .andExpect(jsonPath("$[1].createdAt").exists());

        verify(categoryService).getCategoriesByUserId(userId);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoCategories() throws Exception {

        // Arrange
        Long userId = 5L;

        when(categoryService.getCategoriesByUserId(userId))
                .thenReturn(List.of());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(categoryService).getCategoriesByUserId(userId);
    }

    @Test
    void shouldReturnCategoryWhenItBelongsToAuthenticatedUser() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        CategoryResponse response = new CategoryResponse(
                categoryId,
                "Groceries",
                TransactionType.EXPENSE,
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        when(categoryService.getCategoryById(categoryId, userId))
                .thenReturn(response);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(categoryService).getCategoryById(categoryId, userId);
    }

    @Test
    void shouldReturn404WhenCategoryBelongsToAnotherUser() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 8L;

        when(categoryService.getCategoryById(categoryId, userId))
                .thenThrow(new CategoryNotFoundException());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "8")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));

        verify(categoryService).getCategoryById(categoryId, userId);
    }

    @Test
    void shouldReturn200WhenCategoryIsUpdated() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Food & Groceries",
                TransactionType.EXPENSE
        );

        CategoryResponse response = new CategoryResponse(
                categoryId,
                "Food & Groceries",
                TransactionType.EXPENSE,
                OffsetDateTime.parse("2026-10-14T12:00:00Z")
        );

        when(categoryService.updateCategory(
                eq(categoryId),
                eq(userId),
                any(UpdateCategoryRequest.class)
        )).thenReturn(response);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Food & Groceries"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(categoryService).updateCategory(
                eq(categoryId),
                eq(userId),
                any(UpdateCategoryRequest.class)
        );
    }
    @Test
    void shouldReturn404WhenUpdatingAnotherUsersCategory() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 8L;

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Unauthorized Update",
                TransactionType.EXPENSE
        );

        when(categoryService.updateCategory(
                eq(categoryId),
                eq(userId),
                any(UpdateCategoryRequest.class)
        )).thenThrow(new CategoryNotFoundException());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "8")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));

        verify(categoryService).updateCategory(
                eq(categoryId),
                eq(userId),
                any(UpdateCategoryRequest.class)
        );
    }

    @Test
    void shouldReturn409WhenUpdatingCategoryToDuplicate() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Entertainment",
                TransactionType.EXPENSE
        );

        when(categoryService.updateCategory(
                eq(categoryId),
                eq(userId),
                any(UpdateCategoryRequest.class)
        )).thenThrow(new DuplicateCategoryException());

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Category already exists for this transaction type"
                ));

        verify(categoryService).updateCategory(
                eq(categoryId),
                eq(userId),
                any(UpdateCategoryRequest.class)
        );
    }

    @Test
    void shouldReturn400WhenUpdatingCategoryWithBlankName() throws Exception {

        // Arrange
        Long categoryId = 10L;

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "",
                TransactionType.EXPENSE
        );

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldReturn400WhenUpdatingCategoryWithoutType() throws Exception {

        // Arrange
        Long categoryId = 10L;

        UpdateCategoryRequest request = new UpdateCategoryRequest(
                "Groceries",
                null
        );

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoryService);
    }

    @Test
    void shouldReturn204WhenCategoryIsDeleted() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 5L;

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "5")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(categoryService).deleteCategory(categoryId, userId);
    }

    @Test
    void shouldReturn404WhenDeletingAnotherUsersCategory() throws Exception {

        // Arrange
        Long categoryId = 10L;
        Long userId = 8L;

        doThrow(new CategoryNotFoundException())
                .when(categoryService)
                .deleteCategory(categoryId, userId);

        Jwt jwt = new Jwt(
                "mock-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "8")
        );

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        var securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Category not found"));

        verify(categoryService).deleteCategory(categoryId, userId);
    }
}