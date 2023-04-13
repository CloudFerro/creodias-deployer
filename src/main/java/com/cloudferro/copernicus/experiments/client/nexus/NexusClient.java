package com.cloudferro.copernicus.experiments.client.nexus;

import lombok.AllArgsConstructor;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@AllArgsConstructor
public class NexusClient {

    private final RestTemplate restTemplate;

    public Optional<String> getArtifact(String path) {
        var body = restTemplate.getForEntity("/" + path, String.class).getBody();
        return Optional.ofNullable(body);
    }

}
