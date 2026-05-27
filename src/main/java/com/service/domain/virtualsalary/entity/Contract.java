package com.service.domain.virtualsalary.entity;

import com.service.domain.virtualsalary.enumtype.ContractStatus;
import com.service.domain.virtualsalary.enumtype.TaxType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "CONTRACT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Contract {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "contract_id")
  private Long contractId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "client_name", nullable = false)
  private String clientName;

  @Column(name = "contract_amount", nullable = false)
  private BigDecimal contractAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "tax_type", nullable = false)
  private TaxType taxType;

  @Column(name = "tax_rate", nullable = false)
  private BigDecimal taxRate;

  @Column(name = "expected_payment_date")
  private LocalDate expectedPaymentDate;

  @Column(name = "actual_payment_date")
  private LocalDate actualPaymentDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "contract_status", nullable = false)
  private ContractStatus contractStatus;

  @Column(name = "memo")
  private String memo;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @OneToOne(mappedBy = "contract", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private ContractSettlement settlement;

  @OneToMany(mappedBy = "contract", fetch = FetchType.LAZY)
  private List<PaymentMatching> paymentMatchings = new ArrayList<>();
}
