package com.artem.fakepaymentprovider.repository;


import com.artem.fakepaymentprovider.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByMerchant_IdAndExternalId(Long merchantId, String externalId);

    List<TransactionEntity> findByMerchant_IdAndCreatedAtBetween(
            Long merchantId,
            Instant startDate,
            Instant endDate
    );
    List<TransactionEntity> findByCreatedAtBetween(Instant start, Instant end);

    List<TransactionEntity> findByMerchant_Id(Long merchantId);
}
