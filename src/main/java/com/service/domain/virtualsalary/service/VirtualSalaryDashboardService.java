package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.response.VirtualSalaryDashboardResponse;

public interface VirtualSalaryDashboardService {

  VirtualSalaryDashboardResponse getDashboard(Long userId);
}
