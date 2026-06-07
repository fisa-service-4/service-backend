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
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class ApiCallLogFilter extends OncePerRequestFilter {

  private final AdminLogSaveService adminLogSaveService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String traceId = UUID.randomUUID().toString();
    LocalDateTime requestedAt = LocalDateTime.now();
    long startTime = System.currentTimeMillis();

    try {
      filterChain.doFilter(request, response);
    } finally {
      long durationMs = System.currentTimeMillis() - startTime;
      adminLogSaveService.saveApiCallLog(
          traceId,
          request.getRequestURI(),
          request.getMethod(),
          String.valueOf(response.getStatus()),
          durationMs,
          requestedAt);
    }
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
  }
}
