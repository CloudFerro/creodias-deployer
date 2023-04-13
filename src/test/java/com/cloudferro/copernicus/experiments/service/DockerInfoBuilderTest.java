package com.cloudferro.copernicus.experiments.service;

import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClusterClient.NodePort;
import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClusterClient.ServiceAddress;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DockerInfoBuilderTest {

    @Test
    void shouldBuildDockerinfoJson() {
        var addresses = Set.of(new ServiceAddress("service1", "10.0.0.1",
                new NodePort(8080, 9080),
                new NodePort(8081, 9081)));
        var expected = """
                {
                    "docker_info_list": [
                    {
                        "container_name": "service1",
                        "ip_address": "service1",
                        "port": 8080
                    }]
                }
                """.replaceAll("\\s", "");


        var json = DockerInfoBuilder.dockerInfoJson(addresses);

        assertEquals(expected, json);
    }
}