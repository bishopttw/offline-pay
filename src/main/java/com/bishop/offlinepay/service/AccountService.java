package com.bishop.offlinepay.service;

import com.bishop.offlinepay.config.PasswordEncoderConfig;
import com.bishop.offlinepay.dto.AccountResponse;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bishop.offlinepay.dto.LoginRequest;

import java.math.BigDecimal;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountResponse signup(SignupRequest request){
        if (accountRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Account account = new Account(
                request.getName(),
                request.getEmail(),
                request.getGender(),
                hashedPassword,
                BigDecimal.ZERO
        );
        Account saved = accountRepository.save(account);
        return new AccountResponse(saved);
    }

    public AccountResponse login(LoginRequest request){
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }
        return new AccountResponse(account);
    }
}
