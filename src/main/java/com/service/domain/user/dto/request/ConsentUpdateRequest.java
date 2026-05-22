package com.service.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ConsentUpdateRequest {

  @NotNull private Boolean notificationConsentYn;
}
