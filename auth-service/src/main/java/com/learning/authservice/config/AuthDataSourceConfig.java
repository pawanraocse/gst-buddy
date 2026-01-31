package com.learning.authservice.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * Simplified data source configuration for auth-service (lite version).
 * Single datasource - no multi-tenancy.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.learning.authservice", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
@Slf4j
public class AuthDataSourceConfig {

    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/${POSTGRES_DB_NAME:gst-buddy}}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:postgres}")
    private String username;

    @Value("${spring.datasource.password:postgres}")
    private String password;

    /**
     * Primary DataSource for auth-service.
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        log.info("Configuring data source: {}", jdbcUrl);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(2);
        dataSource.setConnectionTimeout(30000);
        dataSource.setIdleTimeout(600000);
        dataSource.setMaxLifetime(1800000);
        dataSource.setPoolName("auth-service-pool");

        return dataSource;
    }

    /**
     * EntityManagerFactory for JPA entities.
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            DataSource dataSource) {

        log.info("Configuring EntityManagerFactory for auth-service");

        return builder
                .dataSource(dataSource)
                .packages("com.learning.authservice")
                .persistenceUnit("auth")
                .properties(java.util.Map.of(
                        "hibernate.hbm2ddl.auto", "none",
                        "hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect"))
                .build();
    }

    /**
     * Transaction manager for JPA transactions.
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
