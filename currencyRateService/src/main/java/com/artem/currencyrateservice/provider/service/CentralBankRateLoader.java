package com.artem.currencyrateservice.provider.service;



import com.artem.currencyrateservice.entity.ConversionRate;
import com.artem.currencyrateservice.entity.Currency;
import com.artem.currencyrateservice.entity.RateProvider;
import com.artem.currencyrateservice.provider.client.CentralBankClient;
import com.artem.currencyrateservice.provider.dto.CentralBankResponse;
import com.artem.currencyrateservice.provider.dto.CentralBankRate;
import com.artem.currencyrateservice.repository.ConversionRateRepository;
import com.artem.currencyrateservice.repository.CurrencyRepository;
import com.artem.currencyrateservice.repository.RateProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CentralBankRateLoader {

    private final CentralBankClient client;

    private final ConversionRateRepository rateRepository;
    private final CurrencyRepository currencyRepository;
    private final RateProviderRepository providerRepository;

    public void loadRates() {

        CentralBankResponse response = client.getDailyRates();

        Currency rub = currencyRepository
                .findByCode("RUB")
                .orElseThrow();

        RateProvider provider = providerRepository
                .findById("CBR")
                .orElseThrow();

        response.getValute().forEach(rateData -> {

            String code = rateData.getCharCode();

            Currency currency = currencyRepository
                    .findByCode(code)
                    .orElse(null);

            if (currency == null) {
                return;
            }

            BigDecimal value = new BigDecimal(
                    rateData.getValue().replace(",", ".")
            );

            BigDecimal rateValue = value.divide(
                    BigDecimal.valueOf(rateData.getNominal())
            );

            ConversionRate rate = ConversionRate.builder()
                    .sourceCurrency(currency)
                    .destinationCurrency(rub)
                    .rate(rateValue)
                    .rateBeginTime(LocalDateTime.now())
                    .rateEndTime(LocalDateTime.now().plusHours(1))
                    .provider(provider)
                    .createdAt(LocalDateTime.now())
                    .build();

            rateRepository.save(rate);

        });

    }
}
