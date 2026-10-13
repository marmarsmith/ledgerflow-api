
package com.marcel.ledgerflow.repository;

import com.marcel.ledgerflow.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findAllByUserId(Long userId);

    Optional<Account> findByIdAndUserId(Long accountId, Long userId);

    boolean existsByIdAndUserId(Long accountId, Long userId);
}
