package com.artem.transactionservice;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractIntegrationTest {

    public static class FixedPortPostgreSQLContainer
            extends PostgreSQLContainer<FixedPortPostgreSQLContainer> {

        public FixedPortPostgreSQLContainer(
                String dockerImageName
        ) {
            super(dockerImageName);
        }

        public FixedPortPostgreSQLContainer withFixedExposedPort(
                int hostPort,
                int containerPort
        ) {
            super.addFixedExposedPort(
                    hostPort,
                    containerPort
            );
            return this;
        }
    }

    public static final FixedPortPostgreSQLContainer POSTGRES_0 =
            new FixedPortPostgreSQLContainer("postgres:16-alpine")
                    .withFixedExposedPort(65431, 5432)
                    .withDatabaseName("transaction_db_0")
                    .withUsername("postgres")
                    .withPassword("postgres");

    public static final FixedPortPostgreSQLContainer POSTGRES_1 =
            new FixedPortPostgreSQLContainer("postgres:16-alpine")
                    .withFixedExposedPort(65432, 5432)
                    .withDatabaseName("transaction_db_1")
                    .withUsername("postgres")
                    .withPassword("postgres");

    static {
        POSTGRES_0.start();
        POSTGRES_1.start();

        migrate(POSTGRES_0);
        migrate(POSTGRES_1);
    }

    private static void migrate(
            PostgreSQLContainer<?> container
    ) {
        Flyway.configure()
                .dataSource(
                        container.getJdbcUrl(),
                        container.getUsername(),
                        container.getPassword()
                )
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }
}