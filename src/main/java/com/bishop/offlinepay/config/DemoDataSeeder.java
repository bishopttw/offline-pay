package com.bishop.offlinepay.config;

import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Gender;
import com.bishop.offlinepay.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemo;

    public DemoDataSeeder(AccountRepository accountRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.seed-demo:false}") boolean seedDemo) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedDemo = seedDemo;
    }

    @Override
    public void run(String... args) {
        if (!seedDemo || accountRepository.count() > 0) {
            return;
        }
        create("Ada Okoro", "ada@demo.com", Gender.FEMALE, "08011112222", "2000000001", "50000.00");
        create("Tunde Bello", "tunde@demo.com", Gender.MALE, "08033334444", "2000000002", "20000.00");
    }

    private void create(String name, String email, Gender gender,
                        String phone, String accountNumber, String balance) {
        Account account = new Account(name, email, gender,
                passwordEncoder.encode("Demo@12345"), new BigDecimal(balance));
        account.setPinHash(passwordEncoder.encode("4829"));
        account.setPhone(phone);
        account.setAccountNumber(accountNumber);
        accountRepository.save(account);
    }
}
