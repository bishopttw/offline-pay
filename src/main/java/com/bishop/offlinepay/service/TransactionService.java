package com.bishop.offlinepay.service;

import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.model.TransactionStatus;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TransactionService {

    private static final BigDecimal ONLINE_FEE = BigDecimal.valueOf(1.00);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Autowired
    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Transaction transferOnline(Long senderId, Long receiverId, BigDecimal amount) {
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
}