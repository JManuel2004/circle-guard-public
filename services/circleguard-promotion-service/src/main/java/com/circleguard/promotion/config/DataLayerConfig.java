package com.circleguard.promotion.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = "com.circleguard.promotion.repository.jpa"
)
@EnableNeo4jRepositories(
    basePackages = "com.circleguard.promotion.repository.graph",
    transactionManagerRef = "neo4jTransactionManager"
)
public class DataLayerConfig {
    // Explicitly defines the scanning boundaries for the hybrid data layer.
}
