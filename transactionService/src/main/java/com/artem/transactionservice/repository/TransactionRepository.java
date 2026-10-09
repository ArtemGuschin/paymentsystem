package com.artem.transactionservice.repository;

import com.artem.transactionservice.entity.Transaction;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository
        extends JpaRepository<Transaction, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from Transaction t
            where t.uid = :uid
            """)
    Optional<Transaction> findByIdForUpdate(
            @Param("uid") UUID uid
    );
}