package com.bishop.offlinepay.service;

import com.bishop.offlinepay.dto.AccountResponse;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bishop.offlinepay.dto.LoginRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 5;
    private static final String DENIED = "Invalid email or password, or account temporarily locked.";
    private boolean isWeakPin(String pin) {
        return pin.chars().distinct().count() == 1
                || pin.equals("1234") || pin.equals("4321");
    }

    @Autowired
    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AccountResponse signup(SignupRequest request){
        String email = request.getEmail().trim().toLowerCase();

        if (accountRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email is already registered");
        }
        if (isWeakPin(request.getPin())) {
            throw new RuntimeException("Choose a less obvious PIN.");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        Account account = new Account(
                request.getName(),
                email,
                request.getGender(),
                hashedPassword,
                BigDecimal.ZERO
        );
        account.setPinHash(passwordEncoder.encode(request.getPin()));

        Account saved = accountRepository.save(account);
        return new AccountResponse(saved);
    }

    public AccountResponse login(LoginRequest request){
        String email = request.getEmail().trim().toLowerCase();

        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("DENIED"));

        if (isLocked(account)) {
            throw new RuntimeException(DENIED);
        }

        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            registerFailure(account);
            throw new RuntimeException(DENIED);
        }

        resetFailures(account);
        return new AccountResponse(account);
    }

    private boolean isLocked(Account account) {
        return account.getLockUntil() != null
                && account.getLockUntil().isAfter(LocalDateTime.now());
    }

    private void registerFailure(Account account) {
        int attempts = (account.getFailedAttempts() == null ? 0 : account.getFailedAttempts()) + 1;
        if (attempts >= MAX_ATTEMPTS) {
            account.setLockUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            attempts = 0;
        }
        account.setFailedAttempts(attempts);
        accountRepository.save(account);
    }

    private void resetFailures(Account account) {
        if(account.getLockUntil() != null
               || (account.getFailedAttempts() != null && account.getFailedAttempts() > 0)) {
            account.setFailedAttempts(0);
            account.setLockUntil(null);
            accountRepository.save(account);
        }
    }

    public void verifyPin(Long accountId, String pin) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (isLocked(account)) {
            throw new RuntimeException("Too many wrong PINs. Try again in a few minutes.");
        }
        if (pin == null || account.getPinHash() == null
                || !passwordEncoder.matches(pin, account.getPinHash())) {
            int used = (account.getFailedAttempts() == null ? 0 : account.getFailedAttempts()) + 1;
            registerFailure(account);
            int left = MAX_ATTEMPTS - used;
            if (left <= 0) {
                throw new RuntimeException("Too many wrong PINs. Your account is locked for "
                        + LOCK_MINUTES + " minutes.");
            }
            throw new RuntimeException("Wrong PIN. " + left
                    + " attempt(s) left before your account is locked.");
        }
        resetFailures(account);
    }
}
