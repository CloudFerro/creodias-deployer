package com.cloudferro.copernicus.experiments.config;

import com.cloudferro.copernicus.experiments.client.data.CommonDataServiceClient;
import com.cloudferro.copernicus.experiments.client.nexus.NexusClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientsConfiguration {

    @Bean
    CommonDataServiceClient commonDataServiceClient(RestTemplateBuilder restTemplateBuilder,
                                                    CommonDataServiceConfiguration configuration) {
        var restTemplate =  restTemplateBuilder
                .rootUri(configuration.getUrl())
                .basicAuthentication(configuration.getUser(), configuration.getPassword())
                .build();
        return new CommonDataServiceClient(restTemplate);
    }

    @Bean
    NexusClient nexusClient(RestTemplateBuilder restTemplateBuilder,
                                        NexusConfiguration configuration) {
        var restTemplate =  restTemplateBuilder
                .rootUri(configuration.getUrl())
                .basicAuthentication(configuration.getUser(), configuration.getPassword())
                .build();
        return new NexusClient(restTemplate);
    }

}
