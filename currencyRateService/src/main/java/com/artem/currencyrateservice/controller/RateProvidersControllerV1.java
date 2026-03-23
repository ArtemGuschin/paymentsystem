package com.artem.currencyrateservice.controller;



import com.artem.currencyrateservice.api.ProvidersApi;
import com.artem.currencyrateservice.dto.RateProviderResponse;
import com.artem.currencyrateservice.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RateProvidersControllerV1 implements ProvidersApi {

    private final CurrencyRateService service;

    @Override
    public ResponseEntity<List<RateProviderResponse>> getRateProviders() {
        return ResponseEntity.ok(service.getRateProviders());
    }

}
