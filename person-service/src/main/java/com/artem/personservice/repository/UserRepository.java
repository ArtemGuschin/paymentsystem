package com.artem.personservice.repository;




import com.artem.personservice.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.address LEFT JOIN FETCH u.individual WHERE u.id = :id")
    Optional<UserEntity> findByIdWithDetails(UUID id);

    @Query("SELECT u FROM UserEntity u LEFT JOIN FETCH u.address LEFT JOIN FETCH u.individual WHERE u.email = :email")
    Optional<UserEntity> findByEmailWithDetails(String email);
}
