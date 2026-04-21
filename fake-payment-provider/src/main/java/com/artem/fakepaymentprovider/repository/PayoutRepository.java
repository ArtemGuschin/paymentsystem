package com.artem.fakepaymentprovider.repository;


import com.artem.fakepaymentprovider.model.PayoutEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayoutRepository extends JpaRepository<PayoutEntity, Long> {

    Optional<PayoutEntity> findByMerchant_IdAndExternalId(Long merchantId, String externalId);

    List<PayoutEntity> findByMerchant_Id(Long merchantId);
}