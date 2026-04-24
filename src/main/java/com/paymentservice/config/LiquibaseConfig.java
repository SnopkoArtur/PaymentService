package com.paymentservice.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class LiquibaseConfig {

    static {
        System.setProperty("liquibase.secureParsing", "false");
    }

    @Value("${spring.liquibase.mongo-url}")
    private String mongoUri;

    @Value("${spring.liquibase.change-log}")
    private String changeLog;

    @Bean
    public SpringLiquibase liquibase() {
        SpringLiquibase liquibase = new SpringLiquibase() {
            @Override
            public void afterPropertiesSet() {
                try {
                    System.setProperty("liquibase.mongodb.url", mongoUri);

                    Database database = DatabaseFactory.getInstance()
                            .openDatabase(mongoUri, null, null, null, null);

                    try (Liquibase lb = new Liquibase(getChangeLog(), new ClassLoaderResourceAccessor(), database)) {
                        lb.update(new liquibase.Contexts(getContexts()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Liquibase MongoDB initialization failed", e);
                }
            }
        };

        liquibase.setChangeLog(changeLog);
        return liquibase;
    }
}