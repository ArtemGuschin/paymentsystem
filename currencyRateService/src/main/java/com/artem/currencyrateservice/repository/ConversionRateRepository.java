package com.artem.currencyrateservice.repository;

import com.artem.currencyrateservice.entity.ConversionRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConversionRateRepository extends JpaRepository<ConversionRate, Long> {

    @Query("""
        SELECT cr
        FROM ConversionRate cr
        WHERE cr.sourceCurrency.code = :source
          AND cr.destinationCurrency.code = :destination
          AND :now BETWEEN cr.rateBeginTime AND cr.rateEndTime
        ORDER BY cr.rateBeginTime DESC
    """)
    List<ConversionRate> findActualRates(
            @Param("source") String source,
            @Param("destination") String destination,
            @Param("now") LocalDateTime now
    );

}