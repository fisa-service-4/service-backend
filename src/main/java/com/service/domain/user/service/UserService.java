package com.service.domain.user.service;

import com.service.domain.user.dto.request.ConsentUpdateRequest;
import com.service.domain.user.dto.request.ProfileUpdateRequest;
import com.service.domain.user.dto.response.UserResponse;
import com.service.domain.user.entity.User;
import com.service.domain.user.entity.UserProfile;
import com.service.domain.user.repository.UserProfileRepository;
import com.service.domain.user.repository.UserRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserProfileRepository userProfileRepository;

  public UserResponse getMe(Long userId) {
    User user = findUser(userId);
    UserProfile profile = findProfile(userId);
    return UserResponse.of(user, profile);
  }

  @Transactional
  public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
    User user = findUser(userId);
    UserProfile profile = findProfile(userId);
    user.updateUserName(request.getUserName());
    profile.update(request.getFreelancerYn(), request.getJobType());
    return UserResponse.of(user, profile);
  }

  @Transactional
  public void updateConsent(Long userId, ConsentUpdateRequest request) {
    User user = findUser(userId);
    user.updateNotificationConsent(request.getNotificationConsentYn());
  }
  
  @Transactional
  public void withdraw(Long userId) {
    User user = findUser(userId);
    user.withdraw();
  }

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
  }

  private UserProfile findProfile(Long userId) {
    return userProfileRepository
        .findByUserId(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.USER_001));
  }
}
