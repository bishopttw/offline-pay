package com.bishop.offlinepay;

import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.model.TransactionStatus;
import com.bishop.offlinepay.service.TransactionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

class TransactionServiceTest extends BaseServiceTest {

    @Test
    void onlineTransferMovesMoneyAndChargesFee() {
        Account sender = createAccount("s1", "1000.00");
        Account receiver = createAccount("r1", "0.00");

        transactionService.transferOnline(sender.getId(), receiver.getId(), new BigDecimal("200.00"));

        BigDecimal expectedSender = new BigDecimal("1000.00")
                .subtract(new BigDecimal("200.00"))
                .subtract(TransactionService.ONLINE_FEE);
        assertEquals(0, expectedSender.compareTo(balanceOf(sender)));
        assertEquals(0, new BigDecimal("200.00").compareTo(balanceOf(receiver)));
    }

    @Test
    void transferWithNotEnoughMoneyIsRejected() {
        Account sender = createAccount("s2", "50.00");
        Account receiver = createAccount("r2", "0.00");
        assertThrows(RuntimeException.class, () ->
                transactionService.transferOnline(sender.getId(), receiver.getId(), new BigDecimal("100.00")));
        assertEquals(0, new BigDecimal("50.00").compareTo(balanceOf(sender)));
    }

    @Test
    void negativeAmountIsRejected() {
        Account sender = createAccount("s3", "500.00");
        Account receiver = createAccount("r3", "0.00");
        assertThrows(RuntimeException.class, () ->
                transactionService.transferOnline(sender.getId(), receiver.getId(), new BigDecimal("-5.00")));
    }

    @Test
    void offlineTransferWaitsAndDoesNotMoveMoneyYet() {
        Account sender = createAccount("s4", "1000.00");
        Account receiver = createAccount("r4", "0.00");

        Transaction tx = transactionService.transferOffline(
                sender.getId(), receiver.getId(), new BigDecimal("100.00"));

        assertEquals(TransactionStatus.PENDING_SYNC, tx.getStatus());
        assertEquals(0, TransactionService.OFFLINE_FEE.compareTo(tx.getFee()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(balanceOf(sender)));
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceOf(receiver)));
    }

    @Test
    void syncAppliesAWaitingTransfer() {
        Account sender = createAccount("s5", "1000.00");
        Account receiver = createAccount("r5", "0.00");
        Transaction tx = transactionService.transferOffline(
                sender.getId(), receiver.getId(), new BigDecimal("100.00"));

        transactionService.syncPendingTransactions(sender.getId());

        Transaction after = transactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals(TransactionStatus.CONFIRMED, after.getStatus());
        assertEquals(0, new BigDecimal("100.00").compareTo(balanceOf(receiver)));
    }

    @Test
    void syncFailsWhenBalanceDroppedInTheMeantime() {
        Account sender = createAccount("s6", "1000.00");
        Account receiver = createAccount("r6", "0.00");
        Transaction tx = transactionService.transferOffline(
                sender.getId(), receiver.getId(), new BigDecimal("100.00"));

        Account fresh = accountRepository.findById(sender.getId()).orElseThrow();
        fresh.setBalance(new BigDecimal("10.00"));
        accountRepository.save(fresh);

        transactionService.syncPendingTransactions(sender.getId());

        Transaction after = transactionRepository.findById(tx.getId()).orElseThrow();
        assertEquals(TransactionStatus.FAILED, after.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(balanceOf(receiver)));
    }
}
