package com.bishop.offlinepay.dto;

import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Gender;

import java.math.BigDecimal;

public class AccountResponse {

    private Long id;
    private String name;
    private String email;
    private Gender gender;
    private BigDecimal balance;

    public AccountResponse(Account account){
        this.id = account.getId();
        this.name = account.getName();
        this.email = account.getEmail();
        this.gender = account.getGender();
        this.balance = account.getBalance();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public Gender getGender() {
        return gender;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
