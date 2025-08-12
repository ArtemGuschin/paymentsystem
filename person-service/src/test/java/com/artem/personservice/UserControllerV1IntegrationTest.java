package com.artem.personservice;

import com.artem.model.AddressRequest;
import com.artem.model.IndividualRequest;
import com.artem.personservice.dto.UserCreateRequest;
import com.artem.personservice.dto.UserDto;
import com.artem.personservice.dto.UserUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerV1IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1/users";
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Добавьте здесь заголовки авторизации, если необходимо
        return headers;
    }

    private UserCreateRequest createSampleUserRequest() {
        AddressRequest address = new AddressRequest();
        address.setCountryId(1);
        address.setAddressLine("ул. Пушкина, д.10");
        address.setZipCode("123456");
        address.setCity("Москва");
        address.setState("Московская область");

        IndividualRequest individual = new IndividualRequest();
        individual.setPassportNumber("1234567890");
        individual.setPhoneNumber("+79161234567");

        UserCreateRequest request = new UserCreateRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123!");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
//        request.setAddress(address);
//        request.setIndividual(individual);

        return request;
    }

    @BeforeEach
    void setup() {
        // Добавьте здесь инициализацию, если необходимо
    }

    @Test
    void testCreateAndGetUser() {
        // 1. Создание пользователя
        UserCreateRequest createRequest = createSampleUserRequest();
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
                getBaseUrl(),
                new HttpEntity<>(createRequest, createHeaders()),
                UserDto.class
        );

        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        assertNotNull(createResponse.getBody());
        UserDto createdUser = createResponse.getBody();
        assertNotNull(createdUser.getId());

        // 2. Получение пользователя по ID
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
                getBaseUrl() + "/" + createdUser.getId(),
                UserDto.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        UserDto fetchedUser = getResponse.getBody();
        assertNotNull(fetchedUser);
        assertEquals(createdUser.getId(), fetchedUser.getId());
        assertEquals("Иван", fetchedUser.getFirstName());
    }

    @Test
    void testGetUserByEmail() {
        // 1. Создание пользователя
        UserCreateRequest createRequest = createSampleUserRequest();
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
                getBaseUrl(),
                new HttpEntity<>(createRequest, createHeaders()),
                UserDto.class
        );

        UserDto createdUser = createResponse.getBody();
        assertNotNull(createdUser);

        // 2. Получение пользователя по email
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
                getBaseUrl() + "/by-email/test@example.com",
                UserDto.class
        );

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        UserDto fetchedUser = getResponse.getBody();
        assertNotNull(fetchedUser);
        assertEquals(createdUser.getId(), fetchedUser.getId());
    }

    @Test
    void testUpdateUser() {
        // 1. Создание пользователя
        UserCreateRequest createRequest = createSampleUserRequest();
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
                getBaseUrl(),
                new HttpEntity<>(createRequest, createHeaders()),
                UserDto.class
        );

        UserDto createdUser = createResponse.getBody();
        assertNotNull(createdUser);

        // 2. Обновление пользователя
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setFirstName("Петр");
        updateRequest.setLastName("Петров");

        ResponseEntity<UserDto> updateResponse = restTemplate.exchange(
                getBaseUrl() + "/" + createdUser.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, createHeaders()),
                UserDto.class
        );

        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        UserDto updatedUser = updateResponse.getBody();
        assertNotNull(updatedUser);
        assertEquals("Петр", updatedUser.getFirstName());

        // 3. Проверка обновления
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
                getBaseUrl() + "/" + createdUser.getId(),
                UserDto.class
        );

        UserDto fetchedUser = getResponse.getBody();
        assertNotNull(fetchedUser);
        assertEquals("Петр", fetchedUser.getFirstName());
    }

    @Test
    void testDeleteUser() {
        // 1. Создание пользователя
        UserCreateRequest createRequest = createSampleUserRequest();
        ResponseEntity<UserDto> createResponse = restTemplate.postForEntity(
                getBaseUrl(),
                new HttpEntity<>(createRequest, createHeaders()),
                UserDto.class
        );

        UserDto createdUser = createResponse.getBody();
        assertNotNull(createdUser);
        UUID userId = createdUser.getId();

        // 2. Удаление пользователя
        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                getBaseUrl() + "/" + userId,
                HttpMethod.DELETE,
                new HttpEntity<>(createHeaders()),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

        // 3. Проверка удаления
        ResponseEntity<UserDto> getResponse = restTemplate.getForEntity(
                getBaseUrl() + "/" + userId,
                UserDto.class
        );

        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    void testCreateUserValidationFailure() {
        UserCreateRequest invalidRequest = createSampleUserRequest();
        invalidRequest.setEmail("invalid-email"); // Некорректный email

        ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl(),
                new HttpEntity<>(invalidRequest, createHeaders()),
                String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("email"));
    }

    @Test
    void testGetNonExistentUser() {
        UUID nonExistentId = UUID.randomUUID();
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/" + nonExistentId,
                String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}