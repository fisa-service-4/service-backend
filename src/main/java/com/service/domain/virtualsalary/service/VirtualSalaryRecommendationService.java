package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.response.VirtualSalaryRecommendationResponse;

public interface VirtualSalaryRecommendationService {

  VirtualSalaryRecommendationResponse getRecommendation(Long userId);
}
