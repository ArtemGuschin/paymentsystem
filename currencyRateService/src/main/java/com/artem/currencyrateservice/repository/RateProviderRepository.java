package com.artem.currencyrateservice.repository;

import com.artem.currencyrateservice.entity.RateProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RateProviderRepository extends JpaRepository<RateProvider, String> {

    List<RateProvider> findByActiveTrueOrderByPriorityAsc();

}