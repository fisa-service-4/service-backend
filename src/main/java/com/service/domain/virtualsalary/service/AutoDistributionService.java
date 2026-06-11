package com.service.domain.virtualsalary.service;

public interface AutoDistributionService {

  /**
   * 수입에 대한 자동 분배를 실행합니다.
   *
   * @return 분배가 실제로 실행된 경우 true, 설정 없음 등으로 스킵된 경우 false
   */
  boolean distribute(Long userId, Long matchingId);
}
