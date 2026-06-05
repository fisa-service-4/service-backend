package com.service.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.global.response.ApiResponse;
import com.service.global.security.JwtFilter;
import com.service.global.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private static final String[] PUBLIC_URLS = {
    "/api/v1/auth/signup",
    "/api/v1/auth/admin/signup",
    "/api/v1/auth/phone/send",
    "/api/v1/auth/phone/verify",
    "/api/v1/auth/login",
    "/api/v1/auth/reissue",
    "/swagger-ui/**",
    "/v3/api-docs/**",
  };

  private final JwtProvider jwtProvider;
  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(PUBLIC_URLS)
                    .permitAll()
                    .requestMatchers("/api/v1/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          response.setCharacterEncoding("UTF-8");
                          response
                              .getWriter()
                              .write(
                                  objectMapper.writeValueAsString(
                                      ApiResponse.fail("AUTH_005", "유효하지 않은 토큰입니다.")));
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          response.setCharacterEncoding("UTF-8");
                          response
                              .getWriter()
                              .write(
                                  objectMapper.writeValueAsString(
                                      ApiResponse.fail("ADMIN_001", "관리자 권한이 필요합니다.")));
                        }));

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
