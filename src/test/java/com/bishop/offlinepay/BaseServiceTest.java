package com.bishop.offlinepay;


import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Gender;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.repository.TransactionRepository;
import com.bishop.offlinepay.service.AccountService;
import com.bishop.offlinepay.service.TransactionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@SpringBootTest
    @Transactional
    abstract class BaseServiceTest {

        protected static final String PASSWORD = "Passw0rdX";
        protected static final String PIN = "4829";
        private static int phoneCounter = 0;

        @Autowired
        protected AccountService accountService;
        @Autowired protected AccountRepository accountRepository;
        @Autowired protected TransactionService transactionService;
        @Autowired protected TransactionRepository transactionRepository;
        @Autowired protected PasswordEncoder passwordEncoder;

        protected SignupRequest signupRequest(String tag) {
            SignupRequest request = new SignupRequest();
            request.setName("Test User");
            request.setEmail(tag + "@test.com");
            request.setGender(Gender.MALE);
            request.setPassword(PASSWORD);
            request.setPin(PIN);
            request.setPhone(String.format("0801%07d", ++phoneCounter));
            return request;
        }

        protected Account createAccount(String tag, String balance) {
            accountService.signup(signupRequest(tag));
            Account account = accountRepository.findByEmail(tag + "@test.com").orElseThrow();
            account.setBalance(new BigDecimal(balance));
            return accountRepository.save(account);
        }

        protected BigDecimal balanceOf(Account account) {
            return accountRepository.findById(account.getId()).orElseThrow().getBalance();
        }
    }
