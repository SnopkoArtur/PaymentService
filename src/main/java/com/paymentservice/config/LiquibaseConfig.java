package com.paymentservice.config;

import com.mongodb.MongoConfigurationException;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.integration.spring.SpringLiquibase;
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

                    new CommandScope(UpdateCommandStep.COMMAND_NAME)
                            .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, getChangeLog())
                            .addArgumentValue(DbUrlConnectionCommandStep.DATABASE_ARG, database)
                            .execute();
                } catch (Exception e) {
                    throw new MongoConfigurationException("Liquibase MongoDB initialization failed", e);
                }
            }
        };

        liquibase.setChangeLog(changeLog);
        return liquibase;
    }
}