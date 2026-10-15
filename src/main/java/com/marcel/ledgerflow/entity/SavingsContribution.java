package com.marcel.ledgerflow.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;

@Entity
@Table(name = "savings_contributions")
public class SavingsContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "savings_goal_id", nullable = false)
    private SavingsGoal savingsGoal;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Generated
    @Column(
            name = "contributed_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime contributedAt;

    public SavingsContribution() {
    }

    public Long getId() {
        return id;
    }

    public SavingsGoal getSavingsGoal() {
        return savingsGoal;
    }

    public void setSavingsGoal(SavingsGoal savingsGoal) {
        this.savingsGoal = savingsGoal;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getContributedAt() {
        return contributedAt;
    }
}