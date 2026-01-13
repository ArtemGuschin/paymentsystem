package com.artem.transactionservice;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class AbstractIntegrationTest {

    public static class FixedPortPostgreSQLContainer extends PostgreSQLContainer<FixedPortPostgreSQLContainer> {
        public FixedPortPostgreSQLContainer(String dockerImageName) {
            super(dockerImageName);
        }

        public FixedPortPostgreSQLContainer withFixedExposedPort(int hostPort, int containerPort) {
            super.addFixedExposedPort(hostPort, containerPort);
            return this;
        }
    }

    @Container
    public static FixedPortPostgreSQLContainer POSTGRES_0 = new FixedPortPostgreSQLContainer("postgres:latest")
            .withFixedExposedPort(65431, 5432)
            .withDatabaseName("transaction_db_0")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true);

    @Container
    public static FixedPortPostgreSQLContainer POSTGRES_1 = new FixedPortPostgreSQLContainer("postgres:latest")
            .withFixedExposedPort(65432, 5432)
            .withDatabaseName("transaction_db_1")
            .withUsername("postgres")
            .withPassword("postgres")
            .withReuse(true);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.shardingsphere.datasource.ds_0.url", POSTGRES_0::getJdbcUrl);
        registry.add("spring.shardingsphere.datasource.ds_0.username", POSTGRES_0::getUsername);
        registry.add("spring.shardingsphere.datasource.ds_0.password", POSTGRES_0::getPassword);

        registry.add("spring.shardingsphere.datasource.ds_1.url", POSTGRES_1::getJdbcUrl);
        registry.add("spring.shardingsphere.datasource.ds_1.username", POSTGRES_1::getUsername);
        registry.add("spring.shardingsphere.datasource.ds_1.password", POSTGRES_1::getPassword);
    }

    @BeforeAll
    public static void setup() {
        POSTGRES_0.start();
        POSTGRES_1.start();


    }
}