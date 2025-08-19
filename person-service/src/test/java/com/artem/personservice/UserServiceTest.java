package com.artem.personservice;



import com.artem.model.*;
import com.artem.personservice.entity.AddressEntity;
import com.artem.personservice.entity.IndividualEntity;
import com.artem.personservice.entity.UserEntity;
import com.artem.personservice.exception.UserNotFoundException;
import com.artem.personservice.repository.AddressRepository;
import com.artem.personservice.repository.CountryRepository;
import com.artem.personservice.repository.IndividualRepository;
import com.artem.personservice.repository.UserRepository;
import com.artem.personservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private IndividualRepository individualRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private UserService userService;

    private final UUID TEST_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private final String TEST_EMAIL = "test@example.com";

    @Test
    void createUser_ShouldReturnUserResponse() {
        // Arrange
        UserCreateRequest request = createUserRequest();
        AddressEntity addressEntity = createAddressEntity();
        UserEntity userEntity = createUserEntity(addressEntity);
        IndividualEntity individualEntity = createIndividualEntity(userEntity);

        when(addressRepository.save(any(AddressEntity.class))).thenReturn(addressEntity);
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);
        when(individualRepository.save(any(IndividualEntity.class))).thenReturn(individualEntity);

        // Act
        UserResponse response = userService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_EMAIL, response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());

        verify(addressRepository).save(any(AddressEntity.class));
        verify(userRepository).save(any(UserEntity.class));
        verify(individualRepository).save(any(IndividualEntity.class));
    }

    @Test
    void getUserById_ShouldReturnUserResponse() {
        // Arrange
        UserEntity userEntity = createUserEntity(createAddressEntity());
        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(userEntity));

        // Act
        UserResponse response = userService.getUserById(TEST_UUID);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_UUID, response.getId());
        verify(userRepository).findByIdWithDetails(TEST_UUID);
    }

    @Test
    void getUserById_ShouldThrowUserNotFoundException() {
        // Arrange
        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                userService.getUserById(TEST_UUID));
    }

    @Test
    void updateUser_ShouldUpdateExistingUser() {
        // Arrange
        UserEntity existingUser = createUserEntity(createAddressEntity());
        UserUpdateRequest updateRequest = createUpdateRequest();

        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(UserEntity.class)))
                .thenReturn(existingUser);

        // Act
        UserResponse response = userService.updateUser(TEST_UUID, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("updated@example.com", response.getEmail());
        assertEquals("Updated", response.getFirstName());
        verify(userRepository).save(existingUser);
    }

    @Test
    void deleteUser_ShouldDeleteAllEntities() {
        // Arrange
        UserEntity userEntity = createUserEntity(createAddressEntity());
        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(userEntity));

        // Act
        userService.deleteUser(TEST_UUID);

        // Assert
        verify(individualRepository).delete(userEntity.getIndividual());
        verify(userRepository).delete(userEntity);
        verify(addressRepository).delete(userEntity.getAddress());
    }

    private UserCreateRequest createUserRequest() {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail(TEST_EMAIL);
        request.setPassword("password123");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setAddress(createAddressRequest());
        request.setIndividual(createIndividualRequest());
        return request;
    }

    private AddressRequest createAddressRequest() {
        AddressRequest address = new AddressRequest();
        address.setCountryId(1);
        address.setAddressLine("123 Main St");
        address.setZipCode("12345");
        address.setCity("City");
        address.setState("State");
        return address;
    }

    private IndividualRequest createIndividualRequest() {
        IndividualRequest individual = new IndividualRequest();
        individual.setPassportNumber("AB123456");
        individual.setPhoneNumber("+1234567890");
        return individual;
    }

    private UserUpdateRequest createUpdateRequest() {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("updated@example.com");
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setAddress(createAddressRequest());
        request.setIndividual(createIndividualRequest());
        return request;
    }

    private AddressEntity createAddressEntity() {
        AddressEntity address = new AddressEntity();
        address.setId(UUID.randomUUID());
        address.setAddress("123 Main St");
        address.setZipCode("12345");
        address.setCity("City");
        address.setState("State");
        address.setArchived(LocalDateTime.now());
        return address;
    }

    private UserEntity createUserEntity(AddressEntity address) {
        UserEntity user = new UserEntity();
        user.setId(TEST_UUID);
        user.setEmail(TEST_EMAIL);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setAddress(address);
        user.setIndividual(createIndividualEntity(user));
        return user;
    }

    private IndividualEntity createIndividualEntity(UserEntity user) {
        IndividualEntity individual = new IndividualEntity();
        individual.setId(UUID.randomUUID());
        individual.setPassportNumber("AB123456");
        individual.setPhoneNumber("+1234567890");
        individual.setUser(user);
        return individual;
    }
}