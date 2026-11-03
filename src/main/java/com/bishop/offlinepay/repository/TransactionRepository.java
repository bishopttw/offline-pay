package com.bishop.offlinepay.repository;

import com.bishop.offlinepay.model.Transaction;
import com.bishop.offlinepay.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByStatus(TransactionStatus status);
    List<Transaction> findBySenderIdOrReceiverIdOrderByTimestampDesc(Long senderId, Long receiverId);
    List<Transaction> findByStatusAndSenderId(TransactionStatus status, Long senderId);
    Optional<Transaction> findByClientRefAndSenderId(String clientRef, Long senderId);
}