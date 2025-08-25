package com.artem.personservice.service;



import com.artem.model.*;
import com.artem.personservice.entity.AddressEntity;
import com.artem.personservice.entity.IndividualEntity;
import com.artem.personservice.entity.UserEntity;
import com.artem.personservice.exception.UserNotFoundException;
import com.artem.personservice.repository.AddressRepository;
import com.artem.personservice.repository.CountryRepository;
import com.artem.personservice.repository.IndividualRepository;
import com.artem.personservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final IndividualRepository individualRepository;
    private final CountryRepository countryRepository;

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        AddressEntity address = createAddress(request.getAddress());
        address = addressRepository.save(address);

        UserEntity user = createUserEntity(request, address);
        user = userRepository.save(user);

        IndividualEntity individual = createIndividualEntity(request.getIndividual(), user);
        individual.setUser(user); // Установите связь
        individualRepository.save(individual);

        // Обновите пользователя с individual
        user.setIndividual(individual);
        userRepository.save(user);

        return convertToDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        return userRepository.findByIdWithDetails(userId)
                .map(this::convertToDto)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmailWithDetails(email)
                .map(this::convertToDto)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public UserResponse updateUser(UUID userId, UserUpdateRequest request) {
        UserEntity user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Обновление пользователя
        updateUserEntity(user, request);

        // Обновление адреса
        if (request.getAddress() != null && user.getAddress() != null) {
            updateAddressEntity(user.getAddress(), request.getAddress());
        }

        // Обновление индивидуальных данных
        if (request.getIndividual() != null && user.getIndividual() != null) {
            updateIndividualEntity(user.getIndividual(), request.getIndividual());
        }

        userRepository.save(user);
        return convertToDto(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Удаление в правильном порядке
        if (user.getIndividual() != null) {
            individualRepository.delete(user.getIndividual());
        }

        userRepository.delete(user);

        if (user.getAddress() != null) {
            addressRepository.delete(user.getAddress());
        }
    }

    private AddressEntity createAddress(AddressRequest addressRequest) {
        AddressEntity address = new AddressEntity();
        address.setAddress(addressRequest.getAddressLine()); // Используем getAddress(), не getAddressLine()
        address.setZipCode(addressRequest.getZipCode());
        address.setCity(addressRequest.getCity());
        address.setState(addressRequest.getState());
        address.setArchived(LocalDateTime.now());

        // Установка страны
        if (addressRequest.getCountryId() != null) {
            countryRepository.findById(addressRequest.getCountryId())
                    .ifPresentOrElse(
                            address::setCountry,
                            () -> log.warn("Country with id {} not found", addressRequest.getCountryId())
                    );
        }

        return address;
    }

    private UserEntity createUserEntity(UserCreateRequest request, AddressEntity address) {
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setSecretKey(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setFilled(true);
        user.setAddress(address);
        return user;
    }

    private IndividualEntity createIndividualEntity(IndividualRequest individualRequest, UserEntity user) {
        IndividualEntity individual = new IndividualEntity();
        individual.setPassportNumber(individualRequest.getPassportNumber());
        individual.setPhoneNumber(individualRequest.getPhoneNumber());
        individual.setUser(user);
        return individual;
    }

    private void updateUserEntity(UserEntity user, UserUpdateRequest request) {
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        user.setFilled(true);
    }

    private void updateAddressEntity(AddressEntity address, AddressRequest addressRequest) {
        if (addressRequest.getAddressLine() != null) address.setAddress(addressRequest.getAddressLine());
        if (addressRequest.getZipCode() != null) address.setZipCode(addressRequest.getZipCode());
        if (addressRequest.getCity() != null) address.setCity(addressRequest.getCity());
        if (addressRequest.getState() != null) address.setState(addressRequest.getState());

        // Обновление страны
        if (addressRequest.getCountryId() != null) {
            countryRepository.findById(addressRequest.getCountryId())
                    .ifPresent(address::setCountry);
        }
    }

    private void updateIndividualEntity(IndividualEntity individual, IndividualRequest individualRequest) {
        if (individualRequest.getPassportNumber() != null)
            individual.setPassportNumber(individualRequest.getPassportNumber());
        if (individualRequest.getPhoneNumber() != null)
            individual.setPhoneNumber(individualRequest.getPhoneNumber());
    }

    private UserResponse convertToDto(UserEntity user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(OffsetDateTime.from(user.getCreated()));
        dto.setUpdatedAt(user.getUpdated());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());

        if (user.getAddress() != null) {
            dto.setAddress(convertAddressToDto(user.getAddress()));
        }

        if (user.getIndividual() != null) {
            dto.setIndividual(convertIndividualToDto(user.getIndividual()));
        }

        return dto;
    }

    private AddressResponse convertAddressToDto(AddressEntity address) {
        AddressResponse addressDto = new AddressResponse();
        addressDto.setId(address.getId());
        addressDto.setAddressLine(address.getAddress());
        addressDto.setZipCode(address.getZipCode());
        addressDto.setCity(address.getCity());
        addressDto.setState(address.getState());
        if (address.getCountry() != null) {
            addressDto.setCountryId(address.getCountry().getId());
        }

        return addressDto;
    }

    private IndividualResponse convertIndividualToDto(IndividualEntity individual) {
        IndividualResponse individualDto = new IndividualResponse();
        individualDto.setId(individual.getId());
        individualDto.setPassportNumber(individual.getPassportNumber());
        individualDto.setPhoneNumber(individual.getPhoneNumber());
        individualDto.setStatus(individual.getStatus());

        return individualDto;
    }
}

