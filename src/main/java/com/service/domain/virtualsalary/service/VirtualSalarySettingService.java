package com.service.domain.virtualsalary.service;

import com.service.domain.virtualsalary.dto.request.VirtualSalarySettingRequest;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySaveResponse;
import com.service.domain.virtualsalary.dto.response.VirtualSalarySettingResponse;

public interface VirtualSalarySettingService {

  VirtualSalarySettingResponse getSetting(Long userId);

  VirtualSalarySaveResponse saveSetting(Long userId, VirtualSalarySettingRequest request);
}
