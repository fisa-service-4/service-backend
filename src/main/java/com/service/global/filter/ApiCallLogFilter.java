package com.service.global.filter;

import com.service.domain.admin.service.AdminLogSaveService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ApiCallLogFilter extends OncePerRequestFilter {

  private final AdminLogSaveService adminLogSaveService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = request.getHeader("X-Trace-Id");
    if (traceId == null || traceId.isEmpty()) {
      traceId = UUID.randomUUID().toString();
    }
    request.setAttribute("traceId", traceId);
    LocalDateTime requestedAt = LocalDateTime.now();
    long startTime = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      try {
        adminLogSaveService.saveApiCallLog(
            traceId,
            request.getRequestURI(),
            request.getMethod(),
            String.valueOf(response.getStatus()),
            durationMs,
            requestedAt);
      } catch (Exception e) {
        log.error("API 호출 로그 비동기 저장 요청 실패", e);
      }
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getServletPath();
    return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
  }
}
