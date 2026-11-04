package com.bishop.offlinepay;

import com.bishop.offlinepay.dto.SendRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.service.SendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
class SendServiceTest extends BaseServiceTest {

    @Autowired
    private SendService sendService;

    private SendRequest request(String account, String amount, String pin, String ref) {
        SendRequest request = new SendRequest();
        request.setReceiverAccount(account);
        request.setAmount(new BigDecimal(amount));
        request.setPin(pin);
        request.setClientRef(ref);
        return request;
    }

    @Test
    void sameReferenceIsOnlyAppliedOnce() {
        Account sender = createAccount("a1", "1000.00");
        Account receiver = createAccount("b1", "0.00");
        SendRequest send = request(receiver.getAccountNumber(), "100.00", PIN, "ref-123");

        sendService.sendOnline(sender.getId(), send);
        sendService.sendOnline(sender.getId(), send);

        assertEquals(0, new BigDecimal("100.00").compareTo(balanceOf(receiver)));
    }

    @Test
    void wrongPinIsRejectedAndMoneyStays() {
        Account sender = createAccount("a2", "1000.00");
        Account receiver = createAccount("b2", "0.00");
        assertThrows(RuntimeException.class, () -> sendService.sendOnline(sender.getId(),
                request(receiver.getAccountNumber(), "100.00", "0000", "ref-a2")));
        assertEquals(0, new BigDecimal("1000.00").compareTo(balanceOf(sender)));
    }

    @Test
    void fiveWrongPinsLockTheAccountForTransfers() {
        Account sender = createAccount("a3", "1000.00");
        Account receiver = createAccount("b3", "0.00");
        for (int i = 0; i < 5; i++) {
            int n = i;
            assertThrows(RuntimeException.class, () -> sendService.sendOnline(sender.getId(),
                    request(receiver.getAccountNumber(), "10.00", "0000", "ref-a3-" + n)));
        }
        assertThrows(RuntimeException.class, () -> sendService.sendOnline(sender.getId(),
                request(receiver.getAccountNumber(), "10.00", PIN, "ref-a3-ok")));
    }

    @Test
    void cannotSendToYourself() {
        Account sender = createAccount("a4", "1000.00");
        assertThrows(RuntimeException.class, () -> sendService.sendOnline(sender.getId(),
                request(sender.getAccountNumber(), "10.00", PIN, "ref-a4")));
    }

    @Test
    void unknownAccountNumberIsRejected() {
        Account sender = createAccount("a5", "1000.00");
        assertThrows(RuntimeException.class, () -> sendService.sendOnline(sender.getId(),
                request("9999999999", "10.00", PIN, "ref-a5")));
    }
}
