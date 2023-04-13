package com.cloudferro.copernicus.experiments.service;

import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClusterClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;

public class DockerInfoBuilder {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static String dockerInfoJson(Set<KubernetesClusterClient.ServiceAddress> services) {
        var dockerInfo = new DockerInfo(services.stream()
                .map(serviceAddress -> new DockerInfoItem(serviceAddress.name(),
                        serviceAddress.name(),
                        serviceAddress.protobufPort().internal()))
                .toList());
        try {
            return mapper.writeValueAsString(dockerInfo);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Unable to build dockerinfo.json", e);
        }
    }


}
