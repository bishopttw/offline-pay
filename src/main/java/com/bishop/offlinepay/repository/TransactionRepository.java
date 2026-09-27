package com.bishop.offlinepay.repository;

import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByStatus(TransactionStatus status);
}