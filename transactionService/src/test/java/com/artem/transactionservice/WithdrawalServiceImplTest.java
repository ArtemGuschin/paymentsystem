package com.artem.transactionservice;


import com.artem.transaction.model.WithdrawalConfirmRequest;
import com.artem.transaction.model.WithdrawalConfirmResponse;
import com.artem.transactionservice.entity.Transaction;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.repository.TransactionRepository;
import com.artem.transactionservice.service.WalletService;
import com.artem.transactionservice.service.impl.WithdrawalServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private WithdrawalServiceImpl withdrawalService;

    private WithdrawalConfirmRequest request;

    @BeforeEach
    void setUp() {
        request = new WithdrawalConfirmRequest();
        request.setUserUid(UUID.randomUUID());
        request.setWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(100));
    }



    @Test
    void confirm_shouldCreatePendingTransaction_andSendKafkaEvent() {
        UUID userUid = UUID.randomUUID();
        UUID walletUid = UUID.randomUUID();
        BigDecimal amount = BigDecimal.valueOf(100);

        Wallet wallet = new Wallet();
        wallet.setUid(walletUid);
        wallet.setUserUid(userUid);
        wallet.setBalance(BigDecimal.valueOf(1000));


        when(walletService.getActiveWallet(any(UUID.class), any(UUID.class)))
                .thenReturn(wallet);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> {
                    Transaction tx = inv.getArgument(0);
                    tx.setUid(UUID.randomUUID());
                    return tx;
                });

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setUserUid(userUid);
        request.setWalletUid(walletUid);
        request.setAmount(amount);

        WithdrawalConfirmResponse response =
                withdrawalService.confirm(request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getTransactionUid());

        verify(transactionRepository).save(any(Transaction.class));
        verify(kafkaTemplate).send(
                eq("withdrawal.requested"),
                anyString(),
                anyString()
        );
    }




    @Test
    void confirm_shouldThrowException_whenBalanceIsNotEnough() {
        // given
        Wallet wallet = new Wallet();
        wallet.setBalance(BigDecimal.valueOf(50));

        when(walletService.getActiveWallet(any(), any()))
                .thenReturn(wallet);

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setUserUid(UUID.randomUUID());
        request.setWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(100));

        // then
        assertThrows(RuntimeException.class,
                () -> withdrawalService.confirm(request));

        verify(transactionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    

    @Test
    void confirm_shouldThrowException_whenAmountIsNull() {
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setUserUid(UUID.randomUUID());
        request.setWalletUid(UUID.randomUUID());

        assertThrows(RuntimeException.class,
                () -> withdrawalService.confirm(request));
    }

    @Test
    void confirm_shouldThrowException_whenAmountIsNegative() {
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setUserUid(UUID.randomUUID());
        request.setWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(-10));

        assertThrows(RuntimeException.class,
                () -> withdrawalService.confirm(request));
    }
}
