package com.service.global.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.service.domain.admin.repository",
    entityManagerFactoryRef = "logEntityManagerFactory",
    transactionManagerRef = "logTransactionManager")
public class LogJpaConfig {

  @Value("${log.datasource.url}")
  private String logDatasourceUrl;

  @Value("${log.datasource.username}")
  private String logDatasourceUsername;

  @Value("${log.datasource.password}")
  private String logDatasourcePassword;

  @Bean(name = "logDataSource")
  public DataSource logDataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(logDatasourceUrl);
    config.setUsername(logDatasourceUsername);
    config.setPassword(logDatasourcePassword);
    // 로그 DB 미기동 시 앱 시작 실패 방지 (-1: 연결 실패해도 계속 재시도)
    config.setInitializationFailTimeout(-1);
    return new HikariDataSource(config);
  }

  @Bean(name = "logEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean logEntityManagerFactory(
      @Qualifier("logDataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPersistenceUnitName("log");
    em.setPackagesToScan("com.service.domain.admin.entity");

    HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(adapter);

    Map<String, Object> props = new HashMap<>();
    props.put("hibernate.hbm2ddl.auto", "none");
    em.setJpaPropertyMap(props);
    return em;
  }

  @Bean(name = "logTransactionManager")
  public PlatformTransactionManager logTransactionManager(
      @Qualifier("logEntityManagerFactory") EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }

  @Bean
  public DataSourceInitializer logDataSourceInitializer(
      @Qualifier("logDataSource") DataSource dataSource, ResourceLoader resourceLoader) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(resourceLoader.getResource("classpath:sql/log-schema.sql"));
    populator.setContinueOnError(true);

    DataSourceInitializer initializer = new DataSourceInitializer();
    initializer.setDataSource(dataSource);
    initializer.setDatabasePopulator(populator);
    return initializer;
  }
}
