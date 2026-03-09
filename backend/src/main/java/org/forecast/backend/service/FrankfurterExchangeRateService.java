package org.forecast.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.forecast.backend.dtos.exchange.FrankfurterLatestResponse;
import org.forecast.backend.model.ExchangeRates;
import org.forecast.backend.repository.ExchangeRatesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;

@Service
@Primary
@Slf4j
public class FrankfurterExchangeRateService implements ExchangeRateService {

    private final ExchangeRatesRepository exchangeRatesRepository;

    private final WebClient frankfurterClient;

    @Value("${app.currency.base:USD}")
    private String baseCurrency;

    private static final Duration FX_TIMEOUT = Duration.ofSeconds(5);

    public FrankfurterExchangeRateService(
            ExchangeRatesRepository exchangeRatesRepository,
            @Value("${app.fx.frankfurter.base-url:https://api.frankfurter.dev}") String frankfurterBaseUrl
    ) {
        this.exchangeRatesRepository = exchangeRatesRepository;
        this.frankfurterClient = WebClient.builder()
                .baseUrl(frankfurterBaseUrl)
                .build();
    }

    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        String from = normalize(fromCurrency);
        String to = normalize(toCurrency);

        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        return exchangeRatesRepository
                .findFirstByFromCurrencyAndToCurrencyOrderByDateDesc(from, to)
                .map(ExchangeRates::getRate)
                .filter(rate -> rate.signum() > 0)
                .orElseThrow(() -> new IllegalStateException(
                        "No stored FX rate available for " + from + " -> " + to + ". " +
                                "Wait for the daily FX refresh, or run it once on startup."));
    }

    /**
     * Fetches latest rates for configured base currency (app.currency.base) and upserts them into DB.
     * fromCurrency = baseCurrency, toCurrency = each returned currency, rate = returned rate.
     */
    @Transactional
    public int fetchAndUpsertLatestBaseRates() {
        String base = normalize(baseCurrency);
        FrankfurterLatestResponse latest = fetchLatest(base);

        if (latest.rates() == null || latest.rates().isEmpty()) {
            throw new IllegalStateException("Frankfurter API returned no rates");
        }

        Instant now = Instant.now();

        // Load existing rows once and update in-memory to avoid a query per currency.
        List<ExchangeRates> existing = exchangeRatesRepository.findByFromCurrency(base);

        Map<String, ExchangeRates> byToCurrency = existing.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ExchangeRates::getToCurrency,
                        e -> e,
                        (a, b) -> a
                ));

        for (Map.Entry<String, BigDecimal> entry : latest.rates().entrySet()) {
            String to = normalize(entry.getKey());
            BigDecimal rate = entry.getValue();

            if (rate == null || rate.signum() <= 0) {
                log.warn("Skipping invalid FX rate {} -> {}: {}", base, to, rate);
                continue;
            }

            ExchangeRates row = byToCurrency.get(to);
            if (row == null) {
                row = new ExchangeRates();
                row.setFromCurrency(base);
                row.setToCurrency(to);
                byToCurrency.put(to, row);
            }

            row.setRate(rate);
            row.setDate(now);
        }

        exchangeRatesRepository.saveAll(byToCurrency.values());
        return byToCurrency.size();
    }

    private FrankfurterLatestResponse fetchLatest(String base) {
        try {
            FrankfurterLatestResponse response = frankfurterClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/latest")
                            .queryParam("base", base)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, httpResponse ->
                            httpResponse
                                    .bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        String msg = "Frankfurter API returned HTTP " + httpResponse.statusCode() +
                                                (body.isBlank() ? "" : (": " + body));
                                        log.warn("FX fetch failed for base={} -> {}", base, msg);
                                        return new IllegalStateException(msg);
                                    })
                    )
                    .bodyToMono(FrankfurterLatestResponse.class)
                    .timeout(FX_TIMEOUT)
                    .block();

            if (response == null) {
                String msg = "Frankfurter API returned an empty response body";
                log.warn("FX fetch failed for base={}: {}", base, msg);
                throw new IllegalStateException(msg);
            }

            return response;
        } catch (WebClientResponseException ex) {
            String msg = "Frankfurter API returned HTTP " + ex.getStatusCode() + ": " + ex.getResponseBodyAsString();
            log.warn("FX fetch failed for base={}: {}", base, msg, ex);
            throw new IllegalStateException(msg, ex);
        } catch (WebClientRequestException ex) {
            String msg = "Frankfurter API request failed: " + ex.getMessage();
            log.warn("FX fetch failed for base={}: {}", base, msg, ex);
            throw new IllegalStateException(msg, ex);
        } catch (Exception ex) {
            String msg = "Frankfurter API call failed: " + ex.getMessage();
            log.warn("FX fetch failed for base={}: {}", base, msg, ex);
            throw new IllegalStateException(msg, ex);
        }
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
