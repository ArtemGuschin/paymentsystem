package com.artem.transactionservice.repository;

import com.artem.transactionservice.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByUid(UUID uid);

    List<Wallet> findAllByUserUid(UUID userUid);

    List<Wallet> findAllByUserUidAndWalletType_CurrencyCode(
            UUID userUid,
            String currencyCode
    );

    Optional<Wallet> findByUidAndUserUidAndStatus(
            UUID uid,
            UUID userUid,
            String status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Wallet w
        set w.balance = w.balance + :amount,
            w.modifiedAt = CURRENT_TIMESTAMP
        where w.uid = :walletUid
    """)
    int increaseBalance(
            @Param("walletUid") UUID walletUid,
            @Param("amount") BigDecimal amount
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Wallet w
        set w.balance = w.balance - :amount,
            w.modifiedAt = CURRENT_TIMESTAMP
        where w.uid = :walletUid
          and w.balance >= :amount
    """)
    int decreaseBalance(
            @Param("walletUid") UUID walletUid,
            @Param("amount") BigDecimal amount
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select w from Wallet w
    where w.uid = :walletUid
      and w.userUid = :userUid
""")
    Optional<Wallet> findByUidAndUserUidForUpdate(
            @Param("walletUid") UUID walletUid,
            @Param("userUid") UUID userUid
    );
}