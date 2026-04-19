package com.payments.persistence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures 3 separate DataSources — one per PostgreSQL shard.
 * Provides a ShardedJdbcTemplateMap bean so services can get
 * the correct JdbcTemplate for any shard index.
 */
@Configuration
public class ShardedDataSourceConfig {

    // ── Shard 0 (existing PostgreSQL) ──
    @Value("${spring.datasource.shard0.url}")
    private String shard0Url;

    @Value("${spring.datasource.shard0.username}")
    private String shard0Username;

    @Value("${spring.datasource.shard0.password}")
    private String shard0Password;

    // ── Shard 1 ──
    @Value("${spring.datasource.shard1.url}")
    private String shard1Url;

    @Value("${spring.datasource.shard1.username}")
    private String shard1Username;

    @Value("${spring.datasource.shard1.password}")
    private String shard1Password;

    // ── Shard 2 ──
    @Value("${spring.datasource.shard2.url}")
    private String shard2Url;

    @Value("${spring.datasource.shard2.username}")
    private String shard2Username;

    @Value("${spring.datasource.shard2.password}")
    private String shard2Password;

    @Bean
    public DataSource shard0DataSource() {
        return DataSourceBuilder.create()
                .url(shard0Url)
                .username(shard0Username)
                .password(shard0Password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public DataSource shard1DataSource() {
        return DataSourceBuilder.create()
                .url(shard1Url)
                .username(shard1Username)
                .password(shard1Password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public DataSource shard2DataSource() {
        return DataSourceBuilder.create()
                .url(shard2Url)
                .username(shard2Username)
                .password(shard2Password)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    /**
     * Map of shard index → JdbcTemplate.
     * Usage: shardedJdbcTemplates.get(shardRouter.determineShard(txnId))
     */
    @Bean
    public Map<Integer, JdbcTemplate> shardedJdbcTemplates() {
        Map<Integer, JdbcTemplate> map = new HashMap<>();
        map.put(0, new JdbcTemplate(shard0DataSource()));
        map.put(1, new JdbcTemplate(shard1DataSource()));
        map.put(2, new JdbcTemplate(shard2DataSource()));
        return map;
    }
}
