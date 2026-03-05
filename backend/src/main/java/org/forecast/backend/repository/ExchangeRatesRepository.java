package org.forecast.backend.repository;

import org.forecast.backend.model.ExchangeRates;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExchangeRatesRepository extends JpaRepository<ExchangeRates, UUID> {

    Optional<ExchangeRates> findFirstByFromCurrencyAndToCurrencyOrderByDateDesc(String fromCurrency, String toCurrency);

    List<ExchangeRates> findByFromCurrency(String fromCurrency);
}
