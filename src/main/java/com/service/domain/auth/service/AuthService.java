package com.service.domain.auth.service;

import com.service.domain.auth.dto.request.*;
import com.service.domain.auth.dto.response.LoginResponse;
import com.service.domain.auth.dto.response.SignupResponse;
import com.service.domain.auth.dto.response.TokenResponse;
import com.service.domain.auth.entity.PinAuth;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import com.service.global.security.JwtProvider;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private static final String REFRESH_TOKEN_PREFIX = "refresh:";
  private static final String PHONE_CODE_PREFIX = "phone:code:";
  private static final String PHONE_VERIFIED_PREFIX = "phone:verified:";
  private static final long PHONE_CODE_TTL_MINUTES = 5L;
  private static final long REFRESH_TOKEN_TTL_DAYS = 7L;

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;
  private final PinAuthRepository pinAuthRepository;
  private final JwtProvider jwtProvider;
  private final PasswordEncoder passwordEncoder;
  private final StringRedisTemplate redisTemplate;

  @Transactional
  public SignupResponse signup(SignupRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BusinessException(ErrorCode.AUTH_001);
    }
    if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
      throw new BusinessException(ErrorCode.AUTH_002);
    }

    User user =
        User.builder()
            .firebaseUid(UUID.randomUUID().toString())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .userName(request.getUserName())
            .phoneNumber(request.getPhoneNumber())
            .role(User.Role.USER)
            .status(User.Status.INACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(request.getTermsConsentYn())
            .mydataConsentYn(false)
            .build();

    User savedUser = userRepository.save(user);

    UserProfile profile =
        UserProfile.builder()
            .user(savedUser)
            .freelancerYn(request.getFreelancerYn())
            .jobType(request.getJobType())
            .build();

    userProfileRepository.save(profile);

    return SignupResponse.of(savedUser);
  }

  public void sendPhoneVerification(PhoneSendRequest request) {
    String code = String.format("%06d", new SecureRandom().nextInt(1000000));
    redisTemplate
        .opsForValue()
        .set(
            PHONE_CODE_PREFIX + request.getPhoneNumber(),
            code,
            PHONE_CODE_TTL_MINUTES,
            TimeUnit.MINUTES);
    log.info("[SMS] phoneNumber={}, code={}", request.getPhoneNumber(), code);
  }

  public void verifyPhone(PhoneVerifyRequest request) {
    String storedCode =
        redisTemplate.opsForValue().get(PHONE_CODE_PREFIX + request.getPhoneNumber());
    if (storedCode == null) {
      throw new BusinessException(ErrorCode.AUTH_007);
    }
    if (!storedCode.equals(request.getCode())) {
      throw new BusinessException(ErrorCode.AUTH_006);
    }

    redisTemplate.delete(PHONE_CODE_PREFIX + request.getPhoneNumber());
    redisTemplate
        .opsForValue()
        .set(PHONE_VERIFIED_PREFIX + request.getPhoneNumber(), "true", 10, TimeUnit.MINUTES);
  }

  public LoginResponse login(LoginRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_003));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.AUTH_003);
    }

    String accessToken = jwtProvider.generateAccessToken(user.getUserId(), user.getRole().name());
    String refreshToken = jwtProvider.generateRefreshToken(user.getUserId());

    redisTemplate
        .opsForValue()
        .set(
            REFRESH_TOKEN_PREFIX + user.getUserId(),
            refreshToken,
            REFRESH_TOKEN_TTL_DAYS,
            TimeUnit.DAYS);

    return LoginResponse.of(accessToken, refreshToken, user);
  }

  public void logout(Long userId) {
    redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
  }

  public TokenResponse reissue(String refreshToken) {
    jwtProvider.validateRefreshToken(refreshToken);

    Long userId = jwtProvider.getUserId(refreshToken);
    String storedToken = redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + userId);

    if (storedToken == null || !storedToken.equals(refreshToken)) {
      throw new BusinessException(ErrorCode.AUTH_005);
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

    String newAccessToken = jwtProvider.generateAccessToken(userId, user.getRole().name());
    String newRefreshToken = jwtProvider.generateRefreshToken(userId);

    redisTemplate
        .opsForValue()
        .set(REFRESH_TOKEN_PREFIX + userId, newRefreshToken, REFRESH_TOKEN_TTL_DAYS, TimeUnit.DAYS);

    return new TokenResponse(newAccessToken, newRefreshToken);
  }

  @Transactional
  public void registerPin(Long userId, PinRegisterRequest request) {
    validatePinFormat(request.getPin());

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

    if (pinAuthRepository.existsByUserId(userId)) {
      PinAuth existing =
          pinAuthRepository
              .findByUserId(userId)
              .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
      existing.changePin(passwordEncoder.encode(request.getPin()));
      return;
    }

    PinAuth pinAuth =
        PinAuth.builder()
            .user(user)
            .pinHash(passwordEncoder.encode(request.getPin()))
            .failCount(0)
            .lockedYn(false)
            .build();

    pinAuthRepository.save(pinAuth);
  }

  @Transactional
  public void verifyPin(Long userId, PinVerifyRequest request) {
    PinAuth pinAuth =
        pinAuthRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

    if (pinAuth.getLockedYn()) {
      throw new BusinessException(ErrorCode.AUTH_009);
    }
    if (!passwordEncoder.matches(request.getPin(), pinAuth.getPinHash())) {
      pinAuth.fail();
      throw new BusinessException(ErrorCode.AUTH_008);
    }

    pinAuth.resetFailCount();
  }

  @Transactional
  public void changePin(Long userId, PinChangeRequest request) {
    PinAuth pinAuth =
        pinAuthRepository
            .findByUserId(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));

    if (pinAuth.getLockedYn()) {
      throw new BusinessException(ErrorCode.AUTH_009);
    }
    if (!passwordEncoder.matches(request.getCurrentPin(), pinAuth.getPinHash())) {
      pinAuth.fail();
      throw new BusinessException(ErrorCode.AUTH_008);
    }

    validatePinFormat(request.getNewPin());
    pinAuth.changePin(passwordEncoder.encode(request.getNewPin()));
  }

  @Transactional
  public void completeSignup(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
    user.activate();
  }

  private void validatePinFormat(String pin) {
    if (!pin.matches("\\d{6}")) {
      throw new BusinessException(ErrorCode.AUTH_010);
    }
    if (pin.chars().distinct().count() == 1) {
      throw new BusinessException(ErrorCode.AUTH_010);
    }

    boolean ascending = true;
    boolean descending = true;
    for (int i = 0; i < pin.length() - 1; i++) {
      if (pin.charAt(i + 1) - pin.charAt(i) != 1) ascending = false;
      if (pin.charAt(i) - pin.charAt(i + 1) != 1) descending = false;
    }
    if (ascending || descending) {
      throw new BusinessException(ErrorCode.AUTH_010);
    }
  }
}
