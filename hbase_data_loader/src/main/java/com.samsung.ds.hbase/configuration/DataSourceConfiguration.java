package com.samsung.ds.hbase.configuration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class DataSourceConfiguration {

    /**
     * JDBC DataSource
     */
    @Bean(name = "dataSource")
    @Primary
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource() { return DataSourceBuilder.create().type(HikariDataSource.class).build(); }

    /**
     * JDBC Template
     */
    @Bean
    JdbcTemplate jdbcTemplate() { return new JdbcTemplate((dataSource())); }
}
