package com.artem.transactionservice;

import com.artem.transaction.model.CreateWalletRequest;
import com.artem.transaction.model.WalletResponse;
import com.artem.transactionservice.entity.Wallet;
import com.artem.transactionservice.entity.WalletType;
import com.artem.transactionservice.repository.WalletRepository;
import com.artem.transactionservice.repository.WalletTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalletShardingTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    WalletTypeRepository walletTypeRepository;



    @Test
    void shouldCreateWalletAndStoreInCorrectShard() throws Exception {

        UUID userUid = UUID.randomUUID();

        WalletType type = new WalletType();
        type.setName("USD Wallet");
        type.setCurrencyCode("USD");
        type.setStatus("ACTIVE");      // ← ОБЯЗАТЕЛЬНО
        type.setUserType("INDIVIDUAL");

        walletTypeRepository.save(type);

        // ===== 2. ГОТОВИМ REQUEST =====
        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserUid(userUid);
        request.setName("Test Wallet");
        request.setWalletTypeUid(type.getUid()); // ← ВАЖНО

        // ===== 3. ВЫЗОВ КОНТРОЛЛЕРА =====
        var mvcResult = mockMvc.perform(post("/api/v1/wallets/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        Wallet response = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                Wallet.class
        );

        assertEquals("Test Wallet", response.getName());

        // ===== 4. ПРОВЕРЯЕМ GET =====
        mockMvc.perform(get("/api/v1/wallets/{userUid}", userUid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Wallet"));

        // ===== 5. ПРОВЕРЯЕМ БД =====
        List<Wallet> wallets = walletRepository.findAllByUserUid(userUid);

        assertEquals(1, wallets.size());
        assertEquals("Test Wallet", wallets.getFirst().getName());
    }
}
