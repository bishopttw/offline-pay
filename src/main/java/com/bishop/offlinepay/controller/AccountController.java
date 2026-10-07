package com.bishop.offlinepay.controller;

import com.bishop.offlinepay.dto.AccountResponse;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.dto.LoginRequest;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final AccountService accountService;

    @Autowired
    public AccountController(AccountRepository accountRepository, AccountService accountService) {
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    @PostMapping("/signup")
    public AccountResponse signup(@Valid @RequestBody SignupRequest request) {
        return accountService.signup(request);
    }

    @GetMapping
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::new)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return new AccountResponse(account);
    }

    @PostMapping("/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        AccountResponse response = accountService.login(request);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("accountId", response.getId());
        return response;
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if(session !=  null) {
            session.invalidate();
        }
        Map<String, String> result = new HashMap<>();
        result.put("message", "Logged out successfully");
        return result;
    }
}