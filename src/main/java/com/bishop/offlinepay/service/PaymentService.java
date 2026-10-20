package com.bishop.offlinepay.service;

import com.bishop.offlinepay.model.Deposit;
import com.bishop.offlinepay.model.DepositStatus;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.repository.DepositRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PaymentService {

    private static final Logger log =LoggerFactory.getLogger(PaymentService.class);

    private static final BigDecimal MIN_DEPOSIT = new BigDecimal("100");
    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("500000");

    private final AccountRepository accountRepository;
    private final DepositRepository depositRepository;
    private final RestClient restClient;
    private final String secretKey;
    private final String appBaseUrl;

    public PaymentService(AccountRepository accountRepository,
                          DepositRepository depositRepository,
                          @Value("${paystack.secret-key}") String secretKey,
                          @Value("${paystack.base-url}") String paystackBaseUrl,
                          @Value("${app.base-url}") String appBaseUrl) {
        this.accountRepository = accountRepository;
        this.depositRepository = depositRepository;
        this.secretKey = secretKey;
        this.appBaseUrl = appBaseUrl;
        this.restClient = RestClient.builder().baseUrl(paystackBaseUrl).build();
        log.info("Paystack key loaded: {}", secretKey != null && !secretKey.isBlank());
    }

    public String initialize(Long accountId, BigDecimal amount) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new RuntimeException("Payments are not configured yet");
        }
        if (amount == null || amount.compareTo(MIN_DEPOSIT) < 0 || amount.compareTo(MAX_DEPOSIT) > 0) {
            throw new RuntimeException("Amount must be between ₦100 and ₦500,000");
        }
        if (amount.scale() > 2) {
            throw new RuntimeException("Amount cannot have more two decimal places");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        String reference = "OP-" + UUID.randomUUID().toString().replace("-", "");
        Deposit deposit = depositRepository.save(new Deposit(reference, accountId, amount));

        BigDecimal kobo = amount.movePointRight(2);

        Map<String, Object> body = Map.of(
                "email", account.getEmail(),
                "amount", kobo.toBigIntegerExact().toString(),
                "reference", reference,
                "currency", "NGN",
                "callback_url", appBaseUrl + "/payments/callback"
        );

        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/transaction/initialize")
                    .header("Authorization", "Bearer " + secretKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientException ex) {
            log.error("Paystack initialize failed: status={} body={}", ex.getMessage());
            deposit.setStatus(DepositStatus.FAILED);
            depositRepository.save(deposit);
            throw new RuntimeException("Could not start the payment. Please try again.");
        }

        String checkoutUrl = extractCheckoutUrl(response);
        if (checkoutUrl == null) {
            deposit.setStatus(DepositStatus.FAILED);
            depositRepository.save(deposit);
            log.error("Unexpected Paystack response: {}", response);
            throw new RuntimeException("Could not start the payment. Please try again");
        }
        return checkoutUrl;
    }

    @SuppressWarnings("unchecked")
    private String extractCheckoutUrl(Map<String, Object> response) {
        if (response == null || !Boolean.TRUE.equals(response.get("status"))) {
            return null;
        }
        Object data = response.get("data");
        if (!(data instanceof Map)) {
            return null;
        }
        Object url = ((Map<String, Object>) data).get("authorization_url");
        if (!(url instanceof String)) {
            return null;
        }
        String link = (String) url;
        return link.startsWith("https://checkout.paystack.com/") ? link : null;
    }

}
