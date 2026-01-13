package com.artem.transactionservice.repository;

import com.artem.transactionservice.entity.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface WalletTypeRepository extends JpaRepository<WalletType, UUID> {

    Optional<WalletType> findByUid(UUID uid);

    Optional<WalletType> findByName(String name);

}
