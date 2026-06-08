package com.service.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@EnableJpaRepositories(
    basePackages = "com.service.domain.analytics.repository",
    entityManagerFactoryRef = "analyticsEntityManagerFactory",
    transactionManagerRef = "analyticsTransactionManager")
public class AnalyticsJpaConfig {

  @Value("${analytics.datasource.url}")
  private String url;

  @Value("${analytics.datasource.username}")
  private String username;

  @Value("${analytics.datasource.password}")
  private String password;

  @Bean(name = "analyticsDataSource")
  public DataSource analyticsDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(username);
    config.setPassword(password);
    config.setInitializationFailTimeout(-1);
    return new HikariDataSource(config);
  }

  @Bean(name = "analyticsEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean analyticsEntityManagerFactory(
      @Qualifier("analyticsDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPersistenceUnitName("analytics");
    em.setPackagesToScan("com.service.domain.analytics.entity");

    HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(adapter);

    Map<String, Object> props = new HashMap<>();
    props.put("hibernate.hbm2ddl.auto", "none");
    em.setJpaPropertyMap(props);
    return em;
  }

  @Bean(name = "analyticsTransactionManager")
  public PlatformTransactionManager analyticsTransactionManager(
      @Qualifier("analyticsEntityManagerFactory") EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }

  @Bean
  public ApplicationRunner analyticsSchemaInitializer(
      @Qualifier("analyticsDataSource") DataSource dataSource, ResourceLoader resourceLoader) {
    return args -> {
      try {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(resourceLoader.getResource("classpath:sql/analytics-schema.sql"));
        populator.setContinueOnError(true);
        populator.execute(dataSource);
      } catch (Exception e) {
        log.warn("분석 DB 스키마 초기화 실패 - 분석 DB가 미기동 상태이거나 연결할 수 없습니다: {}", e.getMessage());
      }
    };
  }
}
