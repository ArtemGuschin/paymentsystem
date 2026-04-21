package com.artem.fakepaymentprovider.repository;



import com.artem.fakepaymentprovider.model.MerchantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantRepository extends JpaRepository<MerchantEntity, Long> {

    Optional<MerchantEntity> findByMerchantId(String merchantId);
}
