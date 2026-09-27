package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam Long senderId,
            @RequestParam Long receiverId,
            @RequestParam BigDecimal amount) {
        return transactionService.transferOnline(senderId, receiverId, amount);
    }
}