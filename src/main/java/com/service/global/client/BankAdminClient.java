package com.service.global.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class BankAdminClient {

  private final RestTemplate restTemplate;

  @Value("${bank.server.url}")
  private String bankServerUrl;

  public Page<TransferData> getTransferHistory(
      String startDate, String endDate, Pageable pageable) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(bankServerUrl + "/internal/v1/bank/admin/transfers")
            .queryParam("page", pageable.getPageNumber())
            .queryParam("size", pageable.getPageSize());
    if (startDate != null) builder.queryParam("startDate", startDate);
    if (endDate != null) builder.queryParam("endDate", endDate);
    pageable
        .getSort()
        .forEach(
            order ->
                builder.queryParam(
                    "sort", order.getProperty() + "," + order.getDirection().name().toLowerCase()));

    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Trace-Id", UUID.randomUUID().toString());

    ResponseEntity<TransferPageWrapper> response =
        restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<>() {});

    TransferPageWrapper body = response.getBody();
    if (body == null || !body.isSuccess() || body.getData() == null) {
      throw new RuntimeException("bank-server 이체 이력 조회 실패");
    }

    TransferPageData data = body.getData();
    return new PageImpl<>(data.getContent(), pageable, data.getTotalElements());
  }

  @Getter
  @NoArgsConstructor
  static class TransferPageWrapper {
    private boolean success;
    private TransferPageData data;
  }

  @Getter
  @NoArgsConstructor
  static class TransferPageData {
    private List<TransferData> content;
    private long totalElements;
    private int totalPages;
  }

  @Getter
  @NoArgsConstructor
  public static class TransferData {
    private Long transferId;
    private Long fromAccountId;
    private Long fromUserId;
    private String fromAccountNumber;
    private Long toAccountId;
    private Long toUserId;
    private String toAccountNumber;
    private BigDecimal transferAmount;
    private String transferStatus;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
  }
}
