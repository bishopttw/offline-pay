package com.bishop.offlinepay.service;

import com.bishop.offlinepay.model.DepositStatus;
import com.bishop.offlinepay.model.Account;
import com.bishop.offlinepay.model.Deposit;
import com.bishop.offlinepay.repository.AccountRepository;
import com.bishop.offlinepay.repository.DepositRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepositService {

    private final DepositRepository depositRepository;
    private final AccountRepository accountRepository;

    public DepositService(DepositRepository depositRepository,
                          AccountRepository accountRepository) {
        this.depositRepository = depositRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public DepositStatus creditDeposit(String reference, long paidKobo, String currency) {
        Deposit deposit = depositRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Unknown payment reference"));

        // Already credited? Do nothing. This makes refresh and the webhook safe.
        if (deposit.getStatus() == DepositStatus.SUCCESS) {
            return DepositStatus.SUCCESS;
        }

        long expectedKobo = deposit.getAmount().movePointRight(2).longValueExact();
        if (!"NGN".equals(currency) || paidKobo != expectedKobo) {
            deposit.setStatus(DepositStatus.FAILED);
            depositRepository.save(deposit);
            return DepositStatus.FAILED;
        }

        Account account = accountRepository.findById(deposit.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(account.getBalance().add(deposit.getAmount()));
        accountRepository.save(account);

        deposit.setStatus(DepositStatus.SUCCESS);
        depositRepository.save(deposit);
        return DepositStatus.SUCCESS;
    }

    @Transactional
    public void markFailed(String reference) {
        depositRepository.findByReference(reference).ifPresent(deposit -> {
            if (deposit.getStatus() == DepositStatus.PENDING) {
                deposit.setStatus(DepositStatus.FAILED);
                depositRepository.save(deposit);
            }
        });
    }
}
