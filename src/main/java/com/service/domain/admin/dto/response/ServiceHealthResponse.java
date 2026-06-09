package com.service.domain.admin.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceHealthResponse(String serviceName, String status, String error) {}
