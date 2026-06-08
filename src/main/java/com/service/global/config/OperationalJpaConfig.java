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
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = {
      "com.service.domain.auth",
      "com.service.domain.user",
      "com.service.domain.mydata",
      "com.service.domain.notification",
      "com.service.domain.account",
      "com.service.domain.transfer",
      "com.service.domain.stock",
      "com.service.domain.order",
      "com.service.domain.holding",
      "com.service.domain.portfolio",
      "com.service.domain.favoritestock",
      "com.service.domain.virtualsalary"
    },
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager")
public class OperationalJpaConfig {

  @Value("${spring.datasource.url}")
  private String datasourceUrl;

  @Value("${spring.datasource.username}")
  private String datasourceUsername;

  @Value("${spring.datasource.password}")
  private String datasourcePassword;

  @Primary
  @Bean(name = "dataSource")
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(datasourceUrl);
    config.setUsername(datasourceUsername);
    config.setPassword(datasourcePassword);
    return new HikariDataSource(config);
  }

  @Primary
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      @Qualifier("dataSource") DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dataSource);
    em.setPersistenceUnitName("operational");
    em.setPackagesToScan(
        "com.service.domain.auth.entity",
        "com.service.domain.user.entity",
        "com.service.domain.mydata.entity",
        "com.service.domain.notification.entity",
        "com.service.domain.account.entity",
        "com.service.domain.transfer.entity",
        "com.service.domain.stock.entity",
        "com.service.domain.order.entity",
        "com.service.domain.holding.entity",
        "com.service.domain.portfolio.entity",
        "com.service.domain.favoritestock.entity",
        "com.service.domain.virtualsalary.entity");

    HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(adapter);

    Map<String, Object> props = new HashMap<>();
    props.put("hibernate.hbm2ddl.auto", "none");
    em.setJpaPropertyMap(props);
    return em;
  }

  @Primary
  @Bean(name = "transactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
    return new JpaTransactionManager(emf);
  }
}
