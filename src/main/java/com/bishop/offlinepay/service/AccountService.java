package com.bishop.offlinepay.service;

import com.bishop.offlinepay.config.PasswordEncoderConfig;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    public Account signup(SignupRequest request){
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

        return accountRepository.save(account);
    }
}
