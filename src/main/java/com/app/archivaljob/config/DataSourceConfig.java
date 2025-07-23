package com.app.archivaljob.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

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

    // Postgres JdbcTemplate (for batch job reader)
    @Bean(name = "archivalJdbcTemplate")
    public JdbcTemplate archivalJdbcTemplate(@Qualifier("archivalDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // H2 JdbcTemplate (for batch job or other H2 operations)
    @Primary
    @Bean(name = "h2JdbcTemplate")
    public JdbcTemplate h2JdbcTemplate(@Qualifier("dataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
} 