package com.service.domain.virtualsalary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.service.domain.virtualsalary.dto.request.VirtualSalarySettingRequest;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySaveResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySettingResponse;
import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import com.service.domain.virtualsalary.enumtype.VirtualSalaryCategory;
import com.service.domain.virtualsalary.repository.VirtualSalarySettingRepository;
import com.service.global.exception.BusinessException;
import com.service.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class VirtualSalarySettingServiceImplTest {

  @InjectMocks private VirtualSalarySettingServiceImpl settingService;

  @Mock private VirtualSalarySettingRepository settingRepository;

  @Test
  @DisplayName("getSetting - 설정이 존재하면 해당 값을 반환한다")
  void getSetting_returnsSettingWhenExists() {
    VirtualSalarySetting setting =
        VirtualSalarySetting.builder()
            .userId(1L)
            .targetSalary(new BigDecimal("3000000"))
            .payday(25)
            .emergencyTargetAmount(new BigDecimal("5000000"))
            .investmentAmount(new BigDecimal("600000"))
            .emergencyAmount(new BigDecimal("900000"))
            .priorityOrder(
                List.of(
                    VirtualSalaryCategory.SALARY,
                    VirtualSalaryCategory.EMERGENCY,
                    VirtualSalaryCategory.INVESTMENT))
            .build();
    given(settingRepository.findById(1L)).willReturn(Optional.of(setting));

    VirtualSalarySettingResponse response = settingService.getSetting(1L);

    assertThat(response.getTargetSalary()).isEqualByComparingTo(new BigDecimal("3000000"));
    assertThat(response.getPayday()).isEqualTo(25);
    assertThat(response.getPriorityOrder()).hasSize(3);
  }

  @Test
  @DisplayName("getSetting - 설정이 없으면 모든 필드가 null인 빈 응답을 반환한다")
  void getSetting_returnsEmptyResponseWhenNotExists() {
    given(settingRepository.findById(1L)).willReturn(Optional.empty());

    VirtualSalarySettingResponse response = settingService.getSetting(1L);

    assertThat(response.getTargetSalary()).isNull();
    assertThat(response.getPayday()).isNull();
  }

  @Test
  @DisplayName("saveSetting - 설정이 없을 때 신규 생성된다")
  void saveSetting_createsNewSettingWhenNotExists() {
    given(settingRepository.findById(1L)).willReturn(Optional.empty());
    VirtualSalarySettingRequest request =
        buildRequest(new BigDecimal("3000000"), 25, null, null, null, null);

    VirtualSalarySaveResponse response = settingService.saveSetting(1L, request);

    then(settingRepository).should().save(any(VirtualSalarySetting.class));
    assertThat(response.isSaved()).isTrue();
  }

  @Test
  @DisplayName("saveSetting - 설정이 있을 때 기존 설정이 업데이트된다")
  void saveSetting_updatesExistingSettingWhenExists() {
    VirtualSalarySetting existing =
        VirtualSalarySetting.builder()
            .userId(1L)
            .targetSalary(new BigDecimal("2000000"))
            .payday(20)
            .build();
    given(settingRepository.findById(1L)).willReturn(Optional.of(existing));
    VirtualSalarySettingRequest request =
        buildRequest(new BigDecimal("3000000"), 25, null, null, null, null);

    settingService.saveSetting(1L, request);

    then(settingRepository).should(never()).save(any(VirtualSalarySetting.class));
    assertThat(existing.getTargetSalary()).isEqualByComparingTo(new BigDecimal("3000000"));
    assertThat(existing.getPayday()).isEqualTo(25);
  }

  @Test
  @DisplayName("saveSetting - 신규 생성 시 targetSalary가 null이면 VALID_001 예외가 발생한다")
  void saveSetting_throwsExceptionWhenTargetSalaryIsNullForNewSetting() {
    given(settingRepository.findById(1L)).willReturn(Optional.empty());
    VirtualSalarySettingRequest request = buildRequest(null, 25, null, null, null, null);

    assertThatThrownBy(() -> settingService.saveSetting(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.VALID_001);
  }

  @Test
  @DisplayName("saveSetting - 신규 생성 시 payday가 null이면 VALID_001 예외가 발생한다")
  void saveSetting_throwsExceptionWhenPaydayIsNullForNewSetting() {
    given(settingRepository.findById(1L)).willReturn(Optional.empty());
    VirtualSalarySettingRequest request =
        buildRequest(new BigDecimal("3000000"), null, null, null, null, null);

    assertThatThrownBy(() -> settingService.saveSetting(1L, request))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.VALID_001);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private VirtualSalarySettingRequest buildRequest(
      BigDecimal targetSalary,
      Integer payday,
      BigDecimal emergencyTargetAmount,
      BigDecimal investmentAmount,
      BigDecimal emergencyAmount,
      List<VirtualSalaryCategory> priorityOrder) {
    VirtualSalarySettingRequest request = new VirtualSalarySettingRequest();
    ReflectionTestUtils.setField(request, "targetSalary", targetSalary);
    ReflectionTestUtils.setField(request, "payday", payday);
    ReflectionTestUtils.setField(request, "emergencyTargetAmount", emergencyTargetAmount);
    ReflectionTestUtils.setField(request, "investmentAmount", investmentAmount);
    ReflectionTestUtils.setField(request, "emergencyAmount", emergencyAmount);
    ReflectionTestUtils.setField(request, "priorityOrder", priorityOrder);
    return request;
  }
}
