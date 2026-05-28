package com.service.domain.virtualsalary.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VirtualSalarySaveResponse {

  private boolean saved;

  public static VirtualSalarySaveResponse of() {
    return VirtualSalarySaveResponse.builder().saved(true).build();
  }
}
