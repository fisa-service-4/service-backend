package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.VirtualSalarySettingRequest;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySaveResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySettingResponse;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VirtualSalarySettingServiceImpl implements VirtualSalarySettingService {

  private final VirtualSalarySettingRepository virtualSalarySettingRepository;

  @Override
  @Transactional(readOnly = true)
  public VirtualSalarySettingResponse getSetting(Long userId) {
    VirtualSalarySetting setting =
        virtualSalarySettingRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.VIRTUAL_SALARY_001));

    return VirtualSalarySettingResponse.builder()
        .targetSalary(setting.getTargetSalary())
        .payday(setting.getPayday())
        .emergencyTargetAmount(setting.getEmergencyTargetAmount())
        .investmentAmount(setting.getInvestmentAmount())
        .emergencyAmount(setting.getEmergencyAmount())
        .priorityOrder(setting.getPriorityOrder())
        .updatedAt(setting.getUpdatedAt())
        .build();
  }

  @Override
  public VirtualSalarySaveResponse saveSetting(Long userId, VirtualSalarySettingRequest request) {
    Optional<VirtualSalarySetting> existing = virtualSalarySettingRepository.findById(userId);

    if (existing.isPresent()) {
      existing
          .get()
          .update(
              request.getTargetSalary(),
              request.getPayday(),
              request.getEmergencyTargetAmount(),
              request.getInvestmentAmount(),
              request.getEmergencyAmount(),
              request.getPriorityOrder());
    } else {
      if (request.getTargetSalary() == null || request.getPayday() == null) {
        throw new BusinessException(ErrorCode.VALID_001);
      }
      VirtualSalarySetting newSetting =
          VirtualSalarySetting.builder()
              .userId(userId)
              .targetSalary(request.getTargetSalary())
              .payday(request.getPayday())
              .emergencyTargetAmount(request.getEmergencyTargetAmount())
              .investmentAmount(request.getInvestmentAmount())
              .emergencyAmount(request.getEmergencyAmount())
              .priorityOrder(request.getPriorityOrder())
              .build();
      virtualSalarySettingRepository.save(newSetting);
    }

    return VirtualSalarySaveResponse.of();
  }
}
