package com.example.graphqlclient.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpSyncGraphQlClient;
import org.springframework.web.client.RestClient;

@Configuration
public class BackendGraphQlClientConfig {

    /**
     * Builds a synchronous GraphQL client pointing at the backend
     * springboot-graphql-app GraphQL endpoint.
     */
    @Bean
    public HttpSyncGraphQlClient backendGraphQlClient(@Value("${backend.graphql.url}") String backendUrl) {
        RestClient restClient = RestClient.builder()
                .baseUrl(backendUrl)
                .build();
        return HttpSyncGraphQlClient.create(restClient);
    }
}
