package org.forecast.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Currency;


@Service
public class FixedExchangeRateService implements ExchangeRateService {

    private final BigDecimal fallbackRate;

    public FixedExchangeRateService(@Value("${app.fx.fallback-rate:1.0}") BigDecimal fallbackRate) {
        if (fallbackRate == null || fallbackRate.signum() <= 0) {
            throw new IllegalArgumentException("FX fallback rate must be > 0");
        }
        this.fallbackRate = fallbackRate;
    }

    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        // Replace this with a real provider later.
        return fallbackRate;
    }

    private static String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency must be provided");
        }
        String code = currency.trim().toUpperCase();
        // Validate ISO 4217 format early.
        Currency.getInstance(code);
        return code;
    }
}
