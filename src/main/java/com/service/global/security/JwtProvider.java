package com.service.global.security;

import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private final SecretKey secretKey;
  private final long accessTokenExpiration;
  private final long refreshTokenExpiration;

  public JwtProvider(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
      @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    this.accessTokenExpiration = accessTokenExpiration;
    this.refreshTokenExpiration = refreshTokenExpiration;
  }

  public String generateAccessToken(Long userId, String role) {
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("role", role)
        .claim("type", "access")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public String generateRefreshToken(Long userId) {
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("type", "refresh")
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
        .signWith(secretKey)
        .compact();
  }

  public Long getUserId(String token) {
    return Long.parseLong(parseClaims(token).getSubject());
  }

  public String getRole(String token) {
    return parseClaims(token).get("role", String.class);
  }

  public void validateAccessToken(String token) {
    try {
      Claims claims = parseClaims(token);
      if (!"access".equals(claims.get("type"))) {
        throw new BusinessException(ErrorCode.AUTH_005);
      }
    } catch (ExpiredJwtException e) {
      throw new BusinessException(ErrorCode.AUTH_004);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.AUTH_005);
    }
  }

  public void validateRefreshToken(String token) {
    try {
      Claims claims = parseClaims(token);
      if (!"refresh".equals(claims.get("type"))) {
        throw new BusinessException(ErrorCode.AUTH_005);
      }
    } catch (ExpiredJwtException e) {
      throw new BusinessException(ErrorCode.AUTH_004);
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.AUTH_005);
    }
  }

  public boolean isExpired(String token) {
    try {
      parseClaims(token);
      return false;
    } catch (ExpiredJwtException e) {
      return true;
    }
  }

  private Claims parseClaims(String token) {
    try {
      return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    } catch (ExpiredJwtException e) {
      throw e;
    } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.AUTH_005);
    }
  }
}
