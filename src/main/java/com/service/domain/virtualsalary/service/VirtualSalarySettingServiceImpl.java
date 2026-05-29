package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.VirtualSalarySettingRequest;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySaveResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySettingResponse;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
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
        .investmentRatio(setting.getInvestmentRatio())
        .emergencyRatio(setting.getEmergencyRatio())
        .priorityOrder(setting.getPriorityOrder())
        .updatedAt(setting.getUpdatedAt())
        .build();
  }

  @Override
  public VirtualSalarySaveResponse saveSetting(Long userId, VirtualSalarySettingRequest request) {
    validateRatioSum(request.getInvestmentRatio(), request.getEmergencyRatio());

    Optional<VirtualSalarySetting> existing = virtualSalarySettingRepository.findById(userId);

    if (existing.isPresent()) {
      existing
          .get()
          .update(
              request.getTargetSalary(),
              request.getPayday(),
              request.getEmergencyTargetAmount(),
              request.getInvestmentRatio(),
              request.getEmergencyRatio(),
              request.getPriorityOrder());
    } else {
      VirtualSalarySetting newSetting =
          VirtualSalarySetting.builder()
              .userId(userId)
              .targetSalary(request.getTargetSalary())
              .payday(request.getPayday())
              .emergencyTargetAmount(request.getEmergencyTargetAmount())
              .investmentRatio(request.getInvestmentRatio())
              .emergencyRatio(request.getEmergencyRatio())
              .priorityOrder(request.getPriorityOrder())
              .build();
      virtualSalarySettingRepository.save(newSetting);
    }

    return VirtualSalarySaveResponse.of();
  }

  @Override
  public VirtualSalarySaveResponse updateSetting(Long userId, VirtualSalarySettingRequest request) {
    return saveSetting(userId, request);
  }

  private void validateRatioSum(BigDecimal investmentRatio, BigDecimal emergencyRatio) {
    BigDecimal inv = investmentRatio != null ? investmentRatio : BigDecimal.ZERO;
    BigDecimal eme = emergencyRatio != null ? emergencyRatio : BigDecimal.ZERO;
    if (inv.add(eme).compareTo(new BigDecimal("100.00")) > 0) {
      throw new BusinessException(ErrorCode.VIRTUAL_SALARY_002);
    }
  }
}
