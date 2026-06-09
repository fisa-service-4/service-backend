package com.service.domain.auth.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.service.domain.auth.dto.request.*;
import com.service.domain.auth.dto.response.LoginResponse;
import com.service.domain.auth.dto.response.PinStatusResponse;
import com.service.domain.auth.dto.response.SignupResponse;
import com.service.domain.auth.dto.response.TokenResponse;
import com.service.domain.auth.entity.PinAuth;
import com.service.domain.auth.repository.PinAuthRepository;
import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.client.TransactionServerClient;
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
  private final TransactionServerClient transactionServerClient;

  @Transactional
  public SignupResponse adminSignup(AdminSignupRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BusinessException(ErrorCode.AUTH_001);
    }

    String firebaseUid;
    try {
      UserRecord.CreateRequest createRequest =
          new UserRecord.CreateRequest()
              .setEmail(request.getEmail())
              .setPassword(request.getPassword())
              .setDisplayName(request.getUserName());
      UserRecord userRecord;
      try {
        userRecord = FirebaseAuth.getInstance().createUser(createRequest);
      } catch (FirebaseAuthException e) {
        log.warn("[Firebase] 관리자 유저 생성 실패, 기존 유저 조회 시도 email={}", request.getEmail());
        userRecord = FirebaseAuth.getInstance().getUserByEmail(request.getEmail());
      }
      firebaseUid = userRecord.getUid();
    } catch (FirebaseAuthException e) {
      log.error("[Firebase] 관리자 Firebase 계정 생성 실패 email={}", request.getEmail());
      throw new BusinessException(ErrorCode.VALID_001); // 적절한 에러 코드로 변경 필요
    }

    String phonePlaceholder =
        "ADM" + UUID.randomUUID().toString().replace("-", "").substring(0, 17);

    User user =
        User.builder()
            .firebaseUid(firebaseUid)
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .userName(request.getUserName())
            .phoneNumber(phonePlaceholder)
            .role(User.Role.ADMIN)
            .status(User.Status.ACTIVE)
            .notificationConsentYn(false)
            .termsConsentYn(true)
            .mydataConsentYn(false)
            .build();

    User savedUser = userRepository.save(user);

    UserProfile profile =
        UserProfile.builder().user(savedUser).freelancerYn(false).jobType(null).build();

    userProfileRepository.save(profile);

    return SignupResponse.of(savedUser);
  }

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

    completeSignup(savedUser.getUserId());

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

  public PinStatusResponse getPinStatus(Long userId) {
    return pinAuthRepository
        .findByUserId(userId)
        .map(pinAuth -> PinStatusResponse.of(pinAuth.getLockedYn(), pinAuth.getFailCount()))
        .orElse(PinStatusResponse.of(false, 0));
  }

  @Transactional(noRollbackFor = BusinessException.class)
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
      if (Boolean.TRUE.equals(pinAuth.getLockedYn())) {
        pinAuth.getUser().updateStatus(User.Status.LOCKED);
        throw new BusinessException(ErrorCode.AUTH_009);
      }
      throw new BusinessException(ErrorCode.AUTH_008);
    }

    pinAuth.resetFailCount();
  }

  @Transactional(noRollbackFor = BusinessException.class)
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

    try {
      String e164Phone = toE164(user.getPhoneNumber());
      UserRecord.CreateRequest createRequest =
          new UserRecord.CreateRequest()
              .setPhoneNumber(e164Phone)
              .setEmail(user.getEmail())
              .setDisplayName(user.getUserName());

      UserRecord userRecord;
      try {
        userRecord = FirebaseAuth.getInstance().createUser(createRequest);
      } catch (FirebaseAuthException e) {
        log.warn("[Firebase] 유저 생성 실패, 기존 유저 조회 시도 userId={}, message={}", userId, e.getMessage());
        userRecord = FirebaseAuth.getInstance().getUserByEmail(user.getEmail());
      }

      user.updateFirebaseUid(userRecord.getUid());
      try {
        transactionServerClient.linkUser(
            userId, userRecord.getUid(), user.getUserName(), user.getPhoneNumber());
        log.info("[linkUser] transaction-server 연동 완료 userId={}", userId);
      } catch (Exception linkEx) {
        log.warn(
            "[linkUser] transaction-server 연동 실패 userId={}, message={}",
            userId,
            linkEx.getMessage());
      }
    } catch (FirebaseAuthException e) {
      log.error("[Firebase] Firebase 처리 실패 userId={}, message={}", userId, e.getMessage());
    }

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

  private String toE164(String phoneNumber) {
    // 010-XXXX-XXXX 또는 010XXXXXXXX → +8210XXXXXXXX
    String digits = phoneNumber.replaceAll("[^0-9]", "");
    if (digits.startsWith("0")) {
      digits = digits.substring(1);
    }
    return "+82" + digits;
  }
}
