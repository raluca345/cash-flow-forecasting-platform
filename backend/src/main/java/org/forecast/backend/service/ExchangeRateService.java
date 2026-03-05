package org.forecast.backend.service;

import java.math.BigDecimal;


public interface ExchangeRateService {
    BigDecimal getRate(String fromCurrency, String toCurrency);
}

