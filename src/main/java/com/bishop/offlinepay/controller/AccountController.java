package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    @Autowired
    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public Account createAccount(
            @RequestParam String name,
            @RequestParam BigDecimal balance) {
        Account account = new Account(name, balance);
        return accountRepository.save(account);
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/{id}")
    public Account getAccount(@PathVariable Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
}