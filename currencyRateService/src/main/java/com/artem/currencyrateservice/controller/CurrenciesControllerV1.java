package com.artem.currencyrateservice.controller;



import com.artem.currencyrateservice.api.CurrenciesApi;
import com.artem.currencyrateservice.dto.CurrencyResponse;
import com.artem.currencyrateservice.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CurrenciesControllerV1 implements CurrenciesApi {

    private final CurrencyRateService service;

    @Override
    public ResponseEntity<List<CurrencyResponse>> getCurrencies() {
        return ResponseEntity.ok(service.getCurrencies());
    }

}
