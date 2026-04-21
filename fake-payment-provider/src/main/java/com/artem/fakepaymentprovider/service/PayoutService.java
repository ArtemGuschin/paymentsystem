package com.artem.fakepaymentprovider.service;

import com.artem.fakepaymentprovider.dto.Payout;
import com.artem.fakepaymentprovider.dto.PayoutRequest;
import com.artem.fakepaymentprovider.mapper.PayoutMapper;
import com.artem.fakepaymentprovider.model.MerchantEntity;
import com.artem.fakepaymentprovider.model.PayoutEntity;
import com.artem.fakepaymentprovider.repository.MerchantRepository;
import com.artem.fakepaymentprovider.repository.PayoutRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutRepository payoutRepository;
    private final MerchantRepository merchantRepository;
    private final PayoutMapper mapper;

    @Transactional
    public Payout create(PayoutRequest request) {

        String merchantId = getCurrentMerchantId();

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));


        if (request.getExternalId() != null) {
            Optional<PayoutEntity> existing =
                    payoutRepository.findByMerchant_IdAndExternalId(
                            merchant.getId(),
                            request.getExternalId()
                    );

            if (existing.isPresent()) {
                return mapper.toDto(existing.get());
            }
        }

        PayoutEntity entity = mapper.toEntity(request);

        entity.setMerchant(merchant);
        entity.setStatus("PENDING");
        entity.setCreatedAt(Instant.now());

        PayoutEntity saved = payoutRepository.save(entity);

        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public Payout getById(Long id) {

        String merchantId = getCurrentMerchantId();

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        return payoutRepository.findById(id)
                .filter(p -> p.getMerchant().getId().equals(merchant.getId()))
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Payout not found"));
    }

    @Transactional(readOnly = true)
    public List<Payout> getAll() {

        String merchantId = getCurrentMerchantId();

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        return payoutRepository.findByMerchant_Id(merchant.getId())
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    private String getCurrentMerchantId() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }
}
