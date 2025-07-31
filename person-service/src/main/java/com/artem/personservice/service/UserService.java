package com.artem.personservice.service;




import com.artem.personservice.domain.AddressEntity;
import com.artem.personservice.domain.IndividualEntity;
import com.artem.personservice.domain.UserEntity;
import com.artem.personservice.dto.*;
import com.artem.personservice.exception.UserNotFoundException;
import com.artem.personservice.repository.AddressRepository;
import com.artem.personservice.repository.CountryRepository;
import com.artem.personservice.repository.IndividualRepository;
import com.artem.personservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final IndividualRepository individualRepository;
    private final CountryRepository countryRepository;

    @Transactional
    public UserDto createUser(UserCreateRequest request) {
        // Создание адреса
        AddressEntity address = new AddressEntity();
        address.setAddress(request.getAddress().getAddress());
        address.setZipCode(request.getAddress().getZipCode());
        address.setCity(request.getAddress().getCity());
        address.setState(request.getAddress().getState());
        address.setArchived(LocalDateTime.now());

        // Установка страны
        if (request.getAddress().getCountryId() != null) {
            countryRepository.findById(request.getAddress().getCountryId())
                    .ifPresent(address::setCountry);
        }

        address = addressRepository.save(address);

        // Создание пользователя
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setSecretKey(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setFilled(true); // Устанавливаем флаг
        user.setAddress(address);
        user = userRepository.save(user);

        // Создание индивидуальных данных
        IndividualEntity individual = new IndividualEntity();
        individual.setPassportNumber(request.getIndividual().getPassportNumber());
        individual.setPhoneNumber(request.getIndividual().getPhoneNumber());
        individual.setUser(user);
        individualRepository.save(individual);

        return convertToDto(user);
    }

    public UserDto getUserById(UUID userId) {
        return userRepository.findByIdWithDetails(userId)
                .map(this::convertToDto)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    public UserDto getUserByEmail(String email) {
        return userRepository.findByEmailWithDetails(email)
                .map(this::convertToDto)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Transactional
    public UserDto updateUser(UUID userId, UserUpdateRequest request) {
        UserEntity user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Обновление пользователя
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());

        // Обновление адреса
        if (request.getAddress() != null && user.getAddress() != null) {
            AddressEntity address = user.getAddress();
            if (request.getAddress().getAddress() != null) address.setAddress(request.getAddress().getAddress());
            if (request.getAddress().getZipCode() != null) address.setZipCode(request.getAddress().getZipCode());
            if (request.getAddress().getCity() != null) address.setCity(request.getAddress().getCity());
            if (request.getAddress().getState() != null) address.setState(request.getAddress().getState());

            // Обновление страны
            if (request.getAddress().getCountryId() != null) {
                countryRepository.findById(request.getAddress().getCountryId())
                        .ifPresent(address::setCountry);
            }
        }

        // Обновление индивидуальных данных
        if (request.getIndividual() != null && user.getIndividual() != null) {
            IndividualEntity individual = user.getIndividual();
            if (request.getIndividual().getPassportNumber() != null)
                individual.setPassportNumber(request.getIndividual().getPassportNumber());
            if (request.getIndividual().getPhoneNumber() != null)
                individual.setPhoneNumber(request.getIndividual().getPhoneNumber());
        }

        // Обновление флага заполненности
        user.setFilled(true); // Или другая логика обновления

        userRepository.save(user);
        return convertToDto(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = userRepository.findByIdWithDetails(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Удаление в правильном порядке
        if (user.getIndividual() != null) individualRepository.delete(user.getIndividual());
        userRepository.delete(user);
        if (user.getAddress() != null) addressRepository.delete(user.getAddress());
    }

    private UserDto convertToDto(UserEntity user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setCreated(user.getCreated());
        dto.setUpdated(user.getUpdated());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFilled(user.isFilled()); // Исправлено

        if (user.getAddress() != null) {
            AddressDto addressDto = new AddressDto();
            addressDto.setId(user.getAddress().getId());
            addressDto.setAddress(user.getAddress().getAddress());
            addressDto.setZipCode(user.getAddress().getZipCode());
            addressDto.setCity(user.getAddress().getCity());
            addressDto.setState(user.getAddress().getState());
            if (user.getAddress().getCountry() != null) {
                addressDto.setCountryId(user.getAddress().getCountry().getId());
            }
            dto.setAddress(addressDto);
        }

        if (user.getIndividual() != null) {
            IndividualDto individualDto = new IndividualDto();
            individualDto.setId(user.getIndividual().getId());
            individualDto.setPassportNumber(user.getIndividual().getPassportNumber());
            individualDto.setPhoneNumber(user.getIndividual().getPhoneNumber());
            dto.setIndividual(individualDto);
        }

        return dto;
    }
}





