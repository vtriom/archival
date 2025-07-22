package com.app.archivaljob.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @DependsOnDatabaseInitialization
    public DataSourceInitializer batchDataSourceInitializer(@Qualifier("dataSource") DataSource dataSource) {
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(
                new ResourceDatabasePopulator(new ClassPathResource("schema-h2.sql"))
        );
        return initializer;
    }

    // H2 DataSource (default)
    @Primary
    @Bean(name = "dataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    // Postgres DataSource (read-only)
    @Bean(name = "archivalDataSource")
    @ConfigurationProperties(prefix = "archival.postgres")
    public DataSource archivalDataSource() {
        return DataSourceBuilder.create().build();
    }

    // H2 EntityManager
    @Primary
    @Bean(name = "entityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("dataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.app.archivaljob.domain", "com.app.archivaljob.domain.config", "com.app.archivaljob.domain.status")
                .persistenceUnit("h2")
                .build();
    }

    // H2 Transaction Manager
    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory.getObject());
    }

    // Postgres JdbcTemplate (for batch job reader)
    @Bean(name = "archivalJdbcTemplate")
    public JdbcTemplate archivalJdbcTemplate(@Qualifier("archivalDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
} 