package com.service.domain.user.dto.request;

import lombok.Getter;

@Getter
public class ProfileUpdateRequest {

  private String userName;
  private Boolean freelancerYn;
  private String jobType;
}
