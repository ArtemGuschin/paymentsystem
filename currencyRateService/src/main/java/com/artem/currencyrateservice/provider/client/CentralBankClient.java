package com.artem.currencyrateservice.provider.client;



import com.artem.currencyrateservice.provider.dto.CentralBankResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "cbr", url = "${cbr.url}")
public interface CentralBankClient {

    @GetMapping(
            value = "/scripts/XML_daily.asp",
            produces = "application/xml"
    )
    CentralBankResponse getDailyRates();

}