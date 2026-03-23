package com.artem.currencyrateservice.scheduler;



import com.artem.currencyrateservice.provider.service.CentralBankRateLoader;
import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateUpdateScheduler {

    private final CentralBankRateLoader rateLoader;

    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(
            name = "updateCurrencyRates",
            lockAtLeastFor = "PT1M",
            lockAtMostFor = "PT30M"
    )
    public void updateRates() {

        rateLoader.loadRates();

    }
}
