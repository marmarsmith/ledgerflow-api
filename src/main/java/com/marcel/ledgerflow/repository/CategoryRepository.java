package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.marcel.ledgerflow.enums.TransactionType;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUserIdOrderByNameAsc(Long userId);

    Optional<Category> findByIdAndUserId(Long categoryId, Long userId);

    boolean existsByUserIdAndNameAndType(
            Long userId,
            String name,
            TransactionType type
    );
}