package com.cloudferro.copernicus.experiments.client.kubernetes;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.stereotype.Component;

@Component
public class KubernetesClientFactory {

    public KubernetesClusterClient forKubeConfig(String kubeConfig) {
        var config = Config.fromKubeconfig(kubeConfig);
        return new KubernetesClusterClient(new KubernetesClientBuilder().withConfig(config).build());
    }

}
