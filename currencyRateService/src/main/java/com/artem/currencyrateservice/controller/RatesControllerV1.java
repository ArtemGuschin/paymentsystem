package com.artem.currencyrateservice.controller;



import com.artem.currencyrateservice.api.RatesApi;
import com.artem.currencyrateservice.dto.*;
import com.artem.currencyrateservice.service.CurrencyRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequiredArgsConstructor
public class RatesControllerV1 implements RatesApi {

    private final CurrencyRateService service;

    @Override
    public ResponseEntity<RateResponse> getRate(String from, String to, OffsetDateTime timestamp) {
        return ResponseEntity.ok(service.getRate(from, to, timestamp));
    }

    @Override
    public ResponseEntity<ConvertResponse> convertCurrency(ConvertRequest convertRequest) {
        return ResponseEntity.ok(service.convert(convertRequest));
    }

}
