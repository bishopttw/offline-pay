package com.bishop.offlinepay.service;

import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.model.TransactionStatus;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService {

    private static final BigDecimal ONLINE_FEE = new BigDecimal("1.00");
    private static final BigDecimal OFFLINE_FEE = new BigDecimal("5.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    private void validateTransfer(Long senderId, Long receiverId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new RuntimeException("Amount cannot have more than 2 decimal places");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new RuntimeException("Amount is above the maximum allowed per transaction");
        }
        if (senderId.equals(receiverId)) {
            throw new RuntimeException("You cannot send money to yourself");
        }
    }

    @Transactional
    public Transaction transferOnline(Long senderId, Long receiverId, BigDecimal amount) {
        validateTransfer(senderId, receiverId, amount);

        Account sender = accountRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        Account receiver = accountRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        BigDecimal totalDeduction = amount.add(ONLINE_FEE);

        if (sender.getBalance().compareTo(totalDeduction) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(totalDeduction));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction(senderId, receiverId, amount, ONLINE_FEE, TransactionStatus.CONFIRMED);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public Transaction transferOffline(Long senderId, Long receiverId, BigDecimal amount) {
        validateTransfer(senderId, receiverId, amount);

        accountRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender account not found"));
        accountRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        Transaction transaction = new Transaction(senderId, receiverId, amount, OFFLINE_FEE, TransactionStatus.PENDING_SYNC);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public List<Transaction> syncPendingTransactions(Long accountId) {
        List<Transaction> pending = transactionRepository
                .findByStatusAndSenderId(TransactionStatus.PENDING_SYNC, accountId);

        for (Transaction transaction : pending) {
            Account sender = accountRepository.findById(transaction.getSenderId()).orElse(null);
            Account receiver = accountRepository.findById(transaction.getReceiverId()).orElse(null);

            if (sender == null || receiver == null) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                continue;
            }

            BigDecimal totalDeduction = transaction.getAmount().add(transaction.getFee());

            if (sender.getBalance().compareTo(totalDeduction) < 0) {
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
                continue;
            }

            sender.setBalance(sender.getBalance().subtract(totalDeduction));
            receiver.setBalance(receiver.getBalance().add(transaction.getAmount()));

            accountRepository.save(sender);
            accountRepository.save(receiver);

            transaction.setStatus(TransactionStatus.CONFIRMED);
            transactionRepository.save(transaction);
        }

        return pending;
    }
}