package com.service.domain.virtualsalary.repository;

import com.service.domain.virtualsalary.entity.VirtualSalarySetting;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VirtualSalarySettingRepository extends JpaRepository<VirtualSalarySetting, Long> {

  List<VirtualSalarySetting> findAllByPayday(Integer payday);

  List<VirtualSalarySetting> findAllByPaydayGreaterThan(Integer payday);
}
