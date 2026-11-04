package com.bishop.offlinepay;

import com.bishop.offlinepay.dto.LoginRequest;
import com.bishop.offlinepay.dto.SignupRequest;
import com.bishop.offlinepay.model.Account;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest extends BaseServiceTest {

    @Test
    void signupCreatesAccountNumberAndHashesSecrets() {
        Account account = createAccount("alice", "0");
        assertTrue(account.getAccountNumber().matches("\\d{10}"));
        assertNotEquals(PASSWORD, account.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, account.getPassword()));
        assertNotEquals(PIN, account.getPinHash());
        assertTrue(passwordEncoder.matches(PIN, account.getPinHash()));
    }

    @Test
    void duplicateEmailIsRejected() {
        createAccount("bob", "0");
        assertThrows(RuntimeException.class, () -> accountService.signup(signupRequest("bob")));
    }

    @Test
    void weakPinIsRejected() {
        SignupRequest request = signupRequest("carol");
        request.setPin("1111");
        assertThrows(RuntimeException.class, () -> accountService.signup(request));
    }

    @Test
    void emailIsSavedInLowercase() {
        SignupRequest request = signupRequest("dave");
        request.setEmail("Dave@Test.COM");
        accountService.signup(request);
        assertTrue(accountRepository.findByEmail("dave@test.com").isPresent());
    }

    @Test
    void accountLocksAfterFiveWrongPasswords() {
        createAccount("erin", "0");
        LoginRequest wrong = new LoginRequest();
        wrong.setEmail("erin@test.com");
        wrong.setPassword("WrongPass1");
        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> accountService.login(wrong));
        }
        LoginRequest right = new LoginRequest();
        right.setEmail("erin@test.com");
        right.setPassword(PASSWORD);
        assertThrows(RuntimeException.class, () -> accountService.login(right));
    }

    @Test
    void correctLoginWorks() {
        createAccount("frank", "0");
        LoginRequest request = new LoginRequest();
        request.setEmail("frank@test.com");
        request.setPassword(PASSWORD);
        assertNotNull(accountService.login(request));
    }
}
