package com.service.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "휴대폰 인증 요청")
public class PhoneSendRequest {

  @Schema(description = "이름", example = "홍길동")
  private String name;

  @Schema(description = "주민번호 앞 7자리", example = "9001011")
  private String residentNumber;

  @Schema(description = "통신사 (SKT / KT / LGU / SKT_MVNO / KT_MVNO / LGU_MVNO)", example = "KT")
  private String telecom;

  @Schema(description = "휴대폰 번호 (숫자만)", example = "01012341234")
  @NotBlank
  private String phoneNumber;
}
