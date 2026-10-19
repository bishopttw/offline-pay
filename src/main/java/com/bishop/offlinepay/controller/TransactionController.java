package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.service.TransactionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public Transaction transferOnline(
            @RequestParam Long receiverId,
            @RequestParam BigDecimal amount,
            HttpSession session) {
        Long senderId = (Long) session.getAttribute("accountId");
        return transactionService.transferOnline(senderId, receiverId, amount);
    }

    @PostMapping("/transfer-offline")
    public Transaction transferOffline(
            @RequestParam Long receiverId,
            @RequestParam BigDecimal amount,
            HttpSession session) {
        Long senderId = (Long) session.getAttribute("accountId");
        return transactionService.transferOffline(senderId, receiverId, amount);
    }

    @PostMapping("/sync")
    public List<Transaction> sync(HttpSession session) {
        Long accountId = (Long) session.getAttribute("accountId");
        return transactionService.syncPendingTransactions(accountId);
    }
}