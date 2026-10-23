package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.service.DepositService;
import com.bishop.offlinepay.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final PaymentService paymentService;
    private final DepositService depositService;
    private final ObjectMapper objectMapper;

    public  PaymentWebhookController(PaymentService paymentService,
                                     DepositService depositService,
                                     ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.depositService = depositService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {

        if (!paymentService.isValidSignature(rawBody, signature)) {
            return ResponseEntity.status(401).build();
        }

        try{
            Map<?, ?> root = objectMapper.readValue(rawBody, Map.class);
            if ("charge.success".equals(root.get("event"))
                    && root.get("data") instanceof Map<?, ?> data
                    && data.get("reference") instanceof String reference
                    && data.get("amount") instanceof Number amount
                    && data.get("currency") instanceof String currency) {
                depositService.creditDeposit(reference, amount.longValue(), currency);
            }
        } catch (Exception ex) {
            log.error("Webhook handling failed: {}", ex.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
