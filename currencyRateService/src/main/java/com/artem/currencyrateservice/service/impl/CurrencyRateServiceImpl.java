package com.artem.currencyrateservice.service.impl;

import com.artem.currencyrateservice.dto.*;
import com.artem.currencyrateservice.entity.ConversionRate;
import com.artem.currencyrateservice.mapper.*;
import com.artem.currencyrateservice.repository.*;
import com.artem.currencyrateservice.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrencyRateServiceImpl implements CurrencyRateService {

    private final ConversionRateRepository rateRepository;
    private final CurrencyRepository currencyRepository;
    private final RateProviderRepository providerRepository;

    private final RateMapper rateMapper;
    private final CurrencyMapper currencyMapper;
    private final RateProviderMapper providerMapper;
    private final ConvertMapper convertMapper;

    @Override
    public RateResponse getRate(String from, String to, OffsetDateTime timestamp) {

        LocalDateTime time = timestamp == null
                ? LocalDateTime.now()
                : timestamp.toLocalDateTime();

        var direct = rateRepository
                .findActualRates(from, to, time)
                .stream()
                .findFirst();

        if (direct.isPresent()) {
            return rateMapper.toDto(direct.get());
        }

        ConversionRate fromRub = rateRepository
                .findActualRates(from, "RUB", time)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rate not found"));

        ConversionRate toRub = rateRepository
                .findActualRates(to, "RUB", time)
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rate not found"));

        double crossRate =
                fromRub.getRate().doubleValue() /
                        toRub.getRate().doubleValue();

        return new RateResponse()
                .sourceCode(from)
                .destinationCode(to)
                .rate(crossRate)
                .rateTimestamp(OffsetDateTime.now())
                .providerCode("CBR");
    }

    @Override
    public ConvertResponse convert(ConvertRequest request) {

        ConversionRate rate = rateRepository
                .findActualRates(
                        request.getSourceCode(),
                        request.getDestinationCode(),
                        LocalDateTime.now()
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Rate not found"));

        double convertedAmount =
                request.getAmount() * rate.getRate().doubleValue();

        return convertMapper.toDto(
                request,
                convertedAmount,
                rate.getRate().doubleValue()
        );
    }

    @Override
    public List<CurrencyResponse> getCurrencies() {

        return currencyRepository.findAll()
                .stream()
                .map(currencyMapper::toDto)
                .toList();
    }

    @Override
    public List<RateProviderResponse> getRateProviders() {

        return providerRepository.findAll()
                .stream()
                .map(providerMapper::toDto)
                .toList();
    }
}