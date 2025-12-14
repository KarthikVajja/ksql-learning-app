package com.ksqllearning.consumer.config;

import io.confluent.ksql.api.client.Client;
import io.confluent.ksql.api.client.ClientOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KsqlConfig {

    @Value("${ksqldb.server.host}")
    private String ksqlServerHost;

    @Value("${ksqldb.server.port}")
    private int ksqlServerPort;

    @Bean
    public Client ksqlClient() {
        ClientOptions options = ClientOptions.create()
                .setHost(ksqlServerHost)
                .setPort(ksqlServerPort);
        return Client.create(options);
    }
}
