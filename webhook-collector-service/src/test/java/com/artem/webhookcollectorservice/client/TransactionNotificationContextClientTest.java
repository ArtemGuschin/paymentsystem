package com.artem.webhookcollectorservice.client;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TransactionNotificationContextClientTest {

    @Test
    void shouldTimeoutWhenTransactionServiceDoesNotRespond()
            throws Exception {

        // Сервер получил запрос.
        CountDownLatch requestReceived = new CountDownLatch(1);

        // Сервер намеренно не отправляет ответ.
        CountDownLatch releaseResponse = new CountDownLatch(1);

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/internal/transactions", exchange -> {

            requestReceived.countDown();

            try {
                releaseResponse.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });

        server.start();

        try {

            String serverUrl = "http://127.0.0.1:"
                    + server.getAddress().getPort();

            TransactionNotificationContextClient client =
                    new TransactionNotificationContextClient(
                            RestClient.builder(),
                            serverUrl,
                            Duration.ofMillis(500),
                            Duration.ofMillis(300)
                    );

            long start = System.nanoTime();

            // Ожидаем ошибку из-за превышения read timeout.
            assertThrows(
                    ResourceAccessException.class,
                    () -> client.getNotificationContext(
                            UUID.randomUUID()
                    )
            );

            Duration elapsed = Duration.ofNanos(
                    System.nanoTime() - start
            );

            // Проверяем, что запрос действительно дошёл до сервера.
            assertTrue(
                    requestReceived.await(1, TimeUnit.SECONDS)
            );

            // Клиент должен завершиться по таймауту,
            // а не ждать окончания пятиминутной или
            // другой длительной операции.
            assertTrue(
                    elapsed.compareTo(Duration.ofSeconds(3)) < 0,
                    "HTTP request took too long: " + elapsed
            );

        } finally {

            releaseResponse.countDown();
            server.stop(0);
        }
    }
}