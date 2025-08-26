package com.artem.personservice;



import com.artem.model.AddressRequest;
import com.artem.model.IndividualRequest;
import com.artem.model.UserCreateRequest;
import com.artem.model.UserResponse;
import com.artem.model.UserUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class UserControllerV1IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void setUp() {
        baseUrl = "/api/v1/users";
        testEmail = "user" + UUID.randomUUID() + "@example.com";

        // Создаем AddressRequest
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setCountryId(1);
        addressRequest.setAddressLine("ул. Пушкина, д.10");
        addressRequest.setZipCode("123456");
        addressRequest.setCity("Москва");
        addressRequest.setState("Московская область");

        // Создаем IndividualRequest
        IndividualRequest individualRequest = new IndividualRequest();
        individualRequest.setPassportNumber("1234567890");
        individualRequest.setPhoneNumber("+79161234567");

        // Создаем UserCreateRequest
        createRequest = new UserCreateRequest();
        createRequest.setFirstName("Иван");
        createRequest.setLastName("Иванов");
        createRequest.setEmail(testEmail);
        createRequest.setPassword("Str0ngP@ss");
        createRequest.setAddress(addressRequest);
        createRequest.setIndividual(individualRequest);
    }

    private String baseUrl;
    private UserCreateRequest createRequest;
    private String testEmail;

    @Test
    void shouldCreateAndRetrieveUser() {
        // Create user - ожидаем 200 OK согласно OpenAPI
        ResponseEntity<String> createResponse = restTemplate.postForEntity(
                baseUrl,
                createRequest,
                String.class
        );

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());

        // Диагностика: выведем тело ответа
        String responseBody = createResponse.getBody();
        assertNotNull(responseBody, "Response body should not be null");
        System.out.println("Response body: " + responseBody);

        try {
            // Попробуем десериализовать вручную
            UserResponse createdUser = objectMapper.readValue(responseBody, UserResponse.class);
            assertNotNull(createdUser);
            assertNotNull(createdUser.getId());
            assertEquals("Иван", createdUser.getFirstName());
            assertEquals("Иванов", createdUser.getLastName());
            assertEquals(testEmail, createdUser.getEmail());
            assertNotNull(createdUser.getAddress());
            assertNotNull(createdUser.getIndividual());

            // Retrieve by ID
            ResponseEntity<UserResponse> getByIdResponse = restTemplate.getForEntity(
                    baseUrl + "/" + createdUser.getId(),
                    UserResponse.class
            );

            assertEquals(HttpStatus.OK, getByIdResponse.getStatusCode());
            UserResponse foundById = getByIdResponse.getBody();
            assertNotNull(foundById);
            assertEquals(createdUser.getId(), foundById.getId());
            assertEquals(createdUser.getEmail(), foundById.getEmail());

            // Retrieve by Email
            ResponseEntity<UserResponse> getByEmailResponse = restTemplate.getForEntity(
                    baseUrl + "/by-email/" + testEmail,
                    UserResponse.class
            );

            assertEquals(HttpStatus.OK, getByEmailResponse.getStatusCode());
            UserResponse foundByEmail = getByEmailResponse.getBody();
            assertNotNull(foundByEmail);
            assertEquals(createdUser.getId(), foundByEmail.getId());
            assertEquals(createdUser.getEmail(), foundByEmail.getEmail());
        } catch (Exception e) {
            fail("Failed to deserialize response: " + e.getMessage());
        }
    }

    @Test
    void shouldUpdateUser() {
        // Create user
        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
                baseUrl,
                createRequest,
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        UserResponse createdUser = createResponse.getBody();
        assertNotNull(createdUser);

        // Update user
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Петр");
        updateRequest.setLastName("Петров");
        updateRequest.setEmail("updated" + testEmail);

        // Обновляем адрес
        AddressRequest addressRequest = new AddressRequest();
        addressRequest.setCountryId(2);
        addressRequest.setAddressLine("ул. Лермонтова, д.15");
        addressRequest.setZipCode("654321");
        addressRequest.setCity("Санкт-Петербург");
        updateRequest.setAddress(addressRequest);

        HttpEntity<UserUpdateRequest> requestEntity = new HttpEntity<>(updateRequest);

        ResponseEntity<UserResponse> updateResponse = restTemplate.exchange(
                baseUrl + "/" + createdUser.getId(),
                HttpMethod.PUT,
                requestEntity,
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        UserResponse updatedUser = updateResponse.getBody();
        assertNotNull(updatedUser);
        assertEquals(createdUser.getId(), updatedUser.getId());
        assertEquals("Петр", updatedUser.getFirstName());
        assertEquals("Петров", updatedUser.getLastName());
        assertEquals("updated" + testEmail, updatedUser.getEmail());
        assertNotNull(updatedUser.getAddress());
        assertEquals("ул. Лермонтова, д.15", updatedUser.getAddress().getAddressLine());
    }

    @Test
    void shouldDeleteUser() {
        // Create user
        ResponseEntity<UserResponse> createResponse = restTemplate.postForEntity(
                baseUrl,
                createRequest,
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        UserResponse createdUser = createResponse.getBody();
        assertNotNull(createdUser);

        // Delete user
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                baseUrl + "/" + createdUser.getId(),
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // Verify deletion
        ResponseEntity<UserResponse> getResponse = restTemplate.getForEntity(
                baseUrl + "/" + createdUser.getId(),
                UserResponse.class
        );

        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundForInvalidId() {
        UUID invalidId = UUID.randomUUID();

        ResponseEntity<UserResponse> response = restTemplate.getForEntity(
                baseUrl + "/" + invalidId,
                UserResponse.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

