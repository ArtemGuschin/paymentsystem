package com.artem.currencyrateservice.service;


import com.artem.currencyrateservice.dto.*;

import java.time.OffsetDateTime;
import java.util.List;

public interface CurrencyRateService {

    RateResponse getRate(String from, String to, OffsetDateTime timestamp);

    ConvertResponse convert(ConvertRequest request);

    List<CurrencyResponse> getCurrencies();

    List<RateProviderResponse> getRateProviders();

}