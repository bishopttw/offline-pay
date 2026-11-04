package com.bishop.offlinepay;

import com.bishop.offlinepay.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PaymentSignatureTest {

    @Autowired
    private PaymentService paymentService;

    private static final String BODY = "{\"event\":\"charge.success\",\"data\":{\"reference\":\"OP-1\"}}";

    private String sign(String body, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void acceptsACorrectSignature() throws Exception {
        assertTrue(paymentService.isValidSignature(BODY, sign(BODY, "test_secret_key")));
    }

    @Test
    void rejectsASignatureMadeWithTheWrongKey() throws Exception {
        assertFalse(paymentService.isValidSignature(BODY, sign(BODY, "some_other_key")));
    }

    @Test
    void rejectsATamperedBody() throws Exception {
        String signature = sign(BODY, "test_secret_key");
        assertFalse(paymentService.isValidSignature(BODY.replace("OP-1", "OP-2"), signature));
    }

    @Test
    void rejectsAMissingSignature() {
        assertFalse(paymentService.isValidSignature(BODY, null));
    }
}
