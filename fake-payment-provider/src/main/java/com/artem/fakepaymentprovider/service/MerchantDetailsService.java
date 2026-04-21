package com.artem.fakepaymentprovider.service;


import com.artem.fakepaymentprovider.model.MerchantEntity;
import com.artem.fakepaymentprovider.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantDetailsService implements UserDetailsService {

    private final MerchantRepository merchantRepository;



    @Override
    public UserDetails loadUserByUsername(String merchantId) throws UsernameNotFoundException {

        System.out.println(">>> TRY AUTH: " + merchantId);

        MerchantEntity merchant = merchantRepository.findByMerchantId(merchantId)
                .orElseThrow(() -> {
                    System.out.println(">>> NOT FOUND: " + merchantId);
                    return new UsernameNotFoundException("Merchant not found");
                });

        System.out.println(">>> FOUND: " + merchant.getMerchantId());
        System.out.println(">>> PASSWORD FROM DB: " + merchant.getSecretKey());

        return User.builder()
                .username(merchant.getMerchantId())
                .password(merchant.getSecretKey())
                .roles("MERCHANT")
                .build();
    }
}