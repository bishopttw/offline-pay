package com.bishop.offlinepay.service;

import com.bishop.offlinepay.dto.HistoryItem;
import com.bishop.offlinepay.dto.SendRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.model.TransactionStatus;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SendService {

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    public SendService(AccountService accountService,
                       AccountRepository accountRepository,
                       TransactionService transactionService,
                       TransactionRepository transactionRepository) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
        this.transactionRepository = transactionRepository;
    }

    public String lookupName(Long senderId, String email) {
        Account receiver = accountRepository.findByEmail(email.trim().toLowerCase()).orElse(null);
        if (receiver == null || receiver.getId().equals(senderId)){
            return null;
        }
        return maskName(receiver.getName());
    }

    public Transaction sendOnline(Long senderId, SendRequest request) {
        Transaction existing = findExisting(senderId, request.getClientRef());
        if (existing != null){
            return existing;
        }
        accountService.verifyPin(senderId, request.getPin());
        Account receiver = findReceiver(senderId, request.getReceiverEmail());
        Transaction tx = transactionService.transferOnline(
                senderId, receiver.getId(), request.getAmount());
        return  remember(tx, request.getClientRef());
    }

    public List<HistoryItem> history(Long accountId) {
        List<Transaction> all = transactionRepository
                .findBySenderIdOrReceiverIdOrderByTimestampDesc(accountId, accountId);
        Map<Long, String> names = new HashMap<>();
        List<HistoryItem> items = new ArrayList<>();
        for (Transaction tx: all) {
            boolean sent = tx.getSenderId().equals(accountId);
            Long otherId = sent ? tx.getReceiverId() : tx.getSenderId();
            String other = names.computeIfAbsent(otherId, id ->
                    accountRepository.findById(id)
                            .map(a -> maskName(a.getName())).orElse("Unknown"));
            items.add(new HistoryItem(sent ? "SENT" : "RECEIVED", other, tx.getAmount(),
                    sent ? tx.getFee() : BigDecimal.ZERO,
                    tx.getStatus().name(), tx.getTimestamp()));
        }
        return items;
    }

    public int pendingCount(Long accountId) {
        return transactionRepository
                .findByStatusAndSenderId(TransactionStatus.PENDING_SYNC, accountId).size();
    }

    private Transaction findExisting(Long senderId, String clientRef) {
        if (clientRef == null || clientRef.isBlank()) {
            return null;
        }
        return transactionRepository.findByClientRefAndSenderId(clientRef, senderId).orElse(null);
    }

    private Account findReceiver(Long senderId, String email) {
        Account receiver = accountRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("No OfflinePay account with that email."));
        if (receiver.getId().equals(senderId)) {
            throw new RuntimeException("You cannot send money to yourself.");
        }
        return receiver;
    }

    private Transaction remember(Transaction tx, String clientRef){
        if (clientRef != null && !clientRef.isBlank()){
            tx.setClientRef(clientRef);
            return transactionRepository.save(tx);
        }
        return tx;
    }

    private String maskName(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length < 2){
            return parts[0];
        }
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
