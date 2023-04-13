package com.cloudferro.copernicus.experiments.controller;

import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClusterClient;
import org.springframework.lang.NonNull;

public record ServiceDto(@NonNull String name, @NonNull String url) {

    public static ServiceDto fromDomain(KubernetesClusterClient.ServiceAddress serviceAddress) {
        return new ServiceDto(serviceAddress.name(),
                String.format("http://%s:%d", serviceAddress.host(),
                        serviceAddress.webuiPort().external()));
    }
}
