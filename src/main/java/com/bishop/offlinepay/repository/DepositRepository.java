package com.bishop.offlinepay.repository;

import com.bishop.offlinepay.model.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepositRepository extends JpaRepository<Deposit, Long> {
    Optional<Deposit> findByReference(String reference);
}
