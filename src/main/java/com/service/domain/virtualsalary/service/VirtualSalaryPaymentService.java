package com.service.domain.virtualsalary.service;

public interface VirtualSalaryPaymentService {

  void processPayday(int today, boolean isLastDayOfMonth);
}
