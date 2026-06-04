package com.service.domain.virtualsalary.entity;

import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "virtual_salary_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class VirtualSalarySetting {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "target_salary", nullable = false, precision = 18, scale = 2)
    private BigDecimal targetSalary;

    @Column(name = "payday", nullable = false)
    private Integer payday;

    @Column(name = "emergency_target_amount", precision = 18, scale = 2)
    private BigDecimal emergencyTargetAmount;

    @Column(name = "investment_amount", precision = 18, scale = 2)
    private BigDecimal investmentAmount;

    @Column(name = "emergency_amount", precision = 18, scale = 2)
    private BigDecimal emergencyAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "priority_order")
    private List<VirtualSalaryCategory> priorityOrder;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(
            BigDecimal targetSalary,
            Integer payday,
            BigDecimal emergencyTargetAmount,
            BigDecimal investmentAmount,
            BigDecimal emergencyAmount,
            List<VirtualSalaryCategory> priorityOrder) {
        if (targetSalary != null) this.targetSalary = targetSalary;
        if (payday != null) this.payday = payday;
        if (emergencyTargetAmount != null) this.emergencyTargetAmount =
                emergencyTargetAmount;
        if (investmentAmount != null) this.investmentAmount = investmentAmount;
        if (emergencyAmount != null) this.emergencyAmount = emergencyAmount;
        if (priorityOrder != null) this.priorityOrder = priorityOrder;
    }

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
