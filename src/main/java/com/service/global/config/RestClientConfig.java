package com.service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Value("${transaction-server.url}")
  private String transactionServerUrl;

  @Bean
  public RestClient transactionServerRestClient() {
    return RestClient.builder().baseUrl(transactionServerUrl).build();
  }
}
