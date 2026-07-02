package com.artem.paymentservice.client;




import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties(PaymentClientProperties.class)
public class PaymentClientConfiguration {

    @Bean
    public WebClient paymentWebClient(PaymentClientProperties properties) {

        HttpClient httpClient = HttpClient.create()
                .option(
                        ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) properties.getConnectTimeout().toMillis()
                )
                .doOnConnected(connection ->
                        connection.addHandlerLast(
                                new ReadTimeoutHandler(
                                        properties.getReadTimeout().toMillis(),
                                        TimeUnit.MILLISECONDS
                                )
                        )
                );

        return WebClient.builder()
                .baseUrl(properties.getUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    @Bean
    public PaymentClient paymentClient(WebClient paymentWebClient) {
        return new PaymentClientImpl(paymentWebClient);
    }
}