package com.artem.personservice;

import com.artem.model.*;
import com.artem.personservice.entity.AddressEntity;
import com.artem.personservice.entity.CountryEntity;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
        CountryEntity countryEntity = createCountryEntity();

        when(countryRepository.findById(anyInt())).thenReturn(Optional.of(countryEntity));
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
        assertNotNull(response.getAddress());
        assertNotNull(response.getIndividual());

        verify(countryRepository).findById(anyInt());
        verify(addressRepository).save(any(AddressEntity.class));
        verify(userRepository, times(2)).save(any(UserEntity.class));
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
        assertEquals(TEST_EMAIL, response.getEmail());
        assertNotNull(response.getAddress());
        assertNotNull(response.getIndividual());
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
        verify(userRepository).findByIdWithDetails(TEST_UUID);
    }

    @Test
    void getUserByEmail_ShouldReturnUserResponse() {
        // Arrange
        UserEntity userEntity = createUserEntity(createAddressEntity());
        when(userRepository.findByEmailWithDetails(TEST_EMAIL))
                .thenReturn(Optional.of(userEntity));

        // Act
        UserResponse response = userService.getUserByEmail(TEST_EMAIL);

        // Assert
        assertNotNull(response);
        assertEquals(TEST_UUID, response.getId());
        assertEquals(TEST_EMAIL, response.getEmail());
        verify(userRepository).findByEmailWithDetails(TEST_EMAIL);
    }

    @Test
    void getUserByEmail_ShouldThrowUserNotFoundException() {
        // Arrange
        when(userRepository.findByEmailWithDetails(TEST_EMAIL))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                userService.getUserByEmail(TEST_EMAIL));
        verify(userRepository).findByEmailWithDetails(TEST_EMAIL);
    }

    @Test
    void updateUser_ShouldUpdateExistingUser() {
        // Arrange
        UserEntity existingUser = createUserEntity(createAddressEntity());
        UserUpdateRequest updateRequest = createUpdateRequest();
        CountryEntity countryEntity = createCountryEntity();

        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(existingUser));
        when(countryRepository.findById(anyInt())).thenReturn(Optional.of(countryEntity));
        when(userRepository.save(any(UserEntity.class))).thenReturn(existingUser);

        // Act
        UserResponse response = userService.updateUser(TEST_UUID, updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("updated@example.com", response.getEmail());
        assertEquals("Updated", response.getFirstName());
        assertEquals("Name", response.getLastName());
        verify(userRepository).findByIdWithDetails(TEST_UUID);
        verify(userRepository).save(existingUser);
        verify(countryRepository).findById(anyInt());
    }

    @Test
    void deleteUser_ShouldDeleteAllEntities() {
        // Arrange
        AddressEntity addressEntity = createAddressEntity();
        UserEntity userEntity = createUserEntity(addressEntity);

        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(userEntity));
        doNothing().when(individualRepository).delete(any(IndividualEntity.class));
        doNothing().when(userRepository).delete(any(UserEntity.class));
        doNothing().when(addressRepository).delete(any(AddressEntity.class));

        // Act
        userService.deleteUser(TEST_UUID);

        // Assert
        verify(userRepository).findByIdWithDetails(TEST_UUID);
        verify(individualRepository).delete(userEntity.getIndividual());
        verify(userRepository).delete(userEntity);
        verify(addressRepository).delete(userEntity.getAddress());
    }

    @Test
    void deleteUser_ShouldHandleMissingIndividual() {
        // Arrange
        AddressEntity addressEntity = createAddressEntity();
        UserEntity userEntity = createUserEntity(addressEntity);
        userEntity.setIndividual(null);

        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(userEntity));
        doNothing().when(userRepository).delete(any(UserEntity.class));
        doNothing().when(addressRepository).delete(any(AddressEntity.class));

        // Act
        userService.deleteUser(TEST_UUID);

        // Assert
        verify(userRepository).findByIdWithDetails(TEST_UUID);
        verify(individualRepository, never()).delete(any(IndividualEntity.class));
        verify(userRepository).delete(userEntity);
        verify(addressRepository).delete(userEntity.getAddress());
    }

    @Test
    void deleteUser_ShouldHandleMissingAddress() {
        // Arrange
        UserEntity userEntity = createUserEntity(null);

        when(userRepository.findByIdWithDetails(TEST_UUID))
                .thenReturn(Optional.of(userEntity));
        doNothing().when(individualRepository).delete(any(IndividualEntity.class));
        doNothing().when(userRepository).delete(any(UserEntity.class));

        // Act
        userService.deleteUser(TEST_UUID);

        // Assert
        verify(userRepository).findByIdWithDetails(TEST_UUID);
        verify(individualRepository).delete(userEntity.getIndividual());
        verify(userRepository).delete(userEntity);
        verify(addressRepository, never()).delete(any(AddressEntity.class));
    }

    private CountryEntity createCountryEntity() {
        CountryEntity country = new CountryEntity();
        country.setId(1);
        country.setName("Test Country");
//        country.setCode("TC");
        return country;
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
        user.setCreated(OffsetDateTime.now());
        user.setUpdated(OffsetDateTime.now());
        return user;
    }

    private IndividualEntity createIndividualEntity(UserEntity user) {
        IndividualEntity individual = new IndividualEntity();
        individual.setId(UUID.randomUUID());
        individual.setPassportNumber("AB123456");
        individual.setPhoneNumber("+1234567890");
        individual.setUser(user);
        individual.setStatus("ACTIVE");
        return individual;
    }
}