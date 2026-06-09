package com.service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

  @Value("${transaction-server.url}")
  private String transactionServerUrl;

  @Bean
  public RestClient transactionServerRestClient() {
    return RestClient.builder().baseUrl(transactionServerUrl).build();
  }

  @Bean
  public RestClient healthCheckRestClient() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(3000);
    return RestClient.builder().requestFactory(factory).build();
  }
}
