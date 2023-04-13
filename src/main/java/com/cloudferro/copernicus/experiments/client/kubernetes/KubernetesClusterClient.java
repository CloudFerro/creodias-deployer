package com.cloudferro.copernicus.experiments.client.kubernetes;

import com.cloudferro.copernicus.experiments.client.data.DockerContainerDescriptor;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class KubernetesClusterClient implements Closeable {

    public record NodePort(int internal, int external){}
    public record ServiceAddress(String name, String host, NodePort protobufPort, NodePort webuiPort) {}

    private final KubernetesClient client;

    KubernetesClusterClient(KubernetesClient client) {
        this.client = client;
    }

    public void deployToNamespace(Collection<DockerContainerDescriptor> containers, String namespaceName) {
        var namespace = new NamespaceBuilder()
                .withNewMetadata()
                .withName(namespaceName)
                .endMetadata()
                .build();
        client.namespaces().resource(namespace).createOrReplace();
        log.info("Namespace {} created", namespaceName);

        preparePvcs(containers, namespaceName);
        for (var container : containers) {
            var deployment = prepareDeployment(container);
            var service = prepareService(container);
            client.apps().deployments().inNamespace(namespace.getMetadata().getName()).resource(deployment).createOrReplace();
            client.services().inNamespace(namespace.getMetadata().getName()).resource(service).createOrReplace();
        }
    }

    private void preparePvcs(Collection<DockerContainerDescriptor> containers, String namespaceName) {
        var pvs = containers.stream().map(DockerContainerDescriptor::pv)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        for (var pv: pvs) {
            var volumeClaim = new PersistentVolumeClaimBuilder()
                    .withNewMetadata()
                        .withName(pv.name())
                    .endMetadata()
                    .withNewSpec()
                        .withAccessModes("ReadWriteMany")
                        .withNewResources()
                            .withRequests(Map.of("storage", new Quantity("1Gi")))
                        .endResources()
                    .endSpec()
                    .build();
            if (client.persistentVolumeClaims().inNamespace(namespaceName).withName(pv.name()).get() == null) {
                client.persistentVolumeClaims().inNamespace(namespaceName).resource(volumeClaim).createOrReplace();
                log.info("PVC {} created", pv.name());
            } else {
                log.info("PVC {} exists and will not be updated", pv.name());
            }
        }
    }

    private Deployment prepareDeployment(DockerContainerDescriptor containerDescriptor) {
        return new DeploymentBuilder()
                .withNewMetadata()
                    .withName(containerDescriptor.name())
                    .addToLabels("app", containerDescriptor.name())
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .addToMatchLabels("app", containerDescriptor.name())
                    .endSelector()
                    .withNewTemplate()
                        .withMetadata(new ObjectMetaBuilder()
                            .addToLabels("app", containerDescriptor.name())
                            .build())
                        .withNewSpec()
                            .withImagePullSecrets(List.of(new LocalObjectReference("acumos-registry")))
                            .withContainers(List.of(buildContainer(containerDescriptor)))
                            .withVolumes(buildVolume(containerDescriptor))
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }

    private List<Volume> buildVolume(DockerContainerDescriptor containerDescriptor) {
        if (containerDescriptor.pv() != null) {
            return List.of(new VolumeBuilder()
                    .withName(containerDescriptor.name())
                    .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder()
                            .withClaimName(containerDescriptor.pv().name())
                            .build())
                    .build());
        }
        return Collections.emptyList();
    }

    private static Container buildContainer(DockerContainerDescriptor containerDescriptor) {
        var container = new Container();
        container.setName(containerDescriptor.name());
        container.setImage(containerDescriptor.imageUri());

        var protobufPort = new ContainerPort();
        protobufPort.setContainerPort(8061);
        protobufPort.setName("protobuf-api");

        var webuiPort = new ContainerPort();
        webuiPort.setContainerPort(8062);
        webuiPort.setName("webui");

        container.setPorts(List.of(protobufPort, webuiPort));

        if (containerDescriptor.pv() != null) {
            container.setEnv(List.of(new EnvVarBuilder()
                            .withName("SHARED_FOLDER_PATH")
                            .withValue(containerDescriptor.pv().path())
                    .build()));
            container.setVolumeMounts(List.of(new VolumeMountBuilder()
                            .withMountPath(containerDescriptor.pv().path())
                            .withName(containerDescriptor.name())
                    .build()));
        }
        return container;
    }

    private Service prepareService(DockerContainerDescriptor containerDescriptor) {
        return new ServiceBuilder()
                .withNewMetadata()
                .withName(containerDescriptor.name())
                .endMetadata()
                .withNewSpec()
                .addToSelector("app", containerDescriptor.name())
                .withType("NodePort")
                .withPorts(new ServicePortBuilder()
                                .withName("protobuf-api")
                                .withNewTargetPort(8061)
                                .withPort(8061)
                                .build(),
                        new ServicePortBuilder()
                                .withName("webui")
                                .withPort(8062)
                                .withNewTargetPort(8062)
                                .build()
                )
                .endSpec()
                .build();
    }

    public void waitForPods(String namespaceName, Duration waitTimeout) {
        boolean interrupted = false;
        var waitStart = Instant.now();
        while (Instant.now().isBefore(waitStart.plus(waitTimeout)) && !interrupted) {
            var allRunning = client.pods()
                    .inNamespace(namespaceName)
                    .list()
                    .getItems()
                    .stream()
                    .allMatch(pod -> "Running".equals(pod.getStatus().getPhase()));
            if (allRunning) {
                return;
            } else {
                interrupted = sleepOneSec();
            }
        }
        throw new RuntimeException("Orchestrator not ready");
    }

    private static boolean sleepOneSec() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            return true;
        }
        return false;
    }


    public Set<ServiceAddress> getServices(String namespaceName) {

        var firstNodeIp = getNodeExternalAddress();
        return client.services()
                .inNamespace(namespaceName)
                .list()
                .getItems().stream()
                .map(service -> {
                    var protoBufPort = service.getSpec().getPorts().stream()
                            .filter(port -> "protobuf-api".equals(port.getName()))
                            .map(port -> new NodePort(port.getPort(), port.getNodePort()))
                            .findFirst()
                            .orElseThrow();
                    var webUiPort = service.getSpec().getPorts().stream()
                            .filter(port -> "webui".equals(port.getName()))
                            .map(port -> new NodePort(port.getPort(), port.getNodePort()))
                            .findFirst()
                            .orElseThrow();
                    return new ServiceAddress(service.getMetadata().getName(), firstNodeIp, protoBufPort, webUiPort);
                })
                .collect(Collectors.toSet());
    }

    private String getNodeExternalAddress() {
        var addresses = client.nodes().list().getItems().get(0).getStatus().getAddresses();
        return addresses.stream()
                .filter(address -> "ExternalIP".equalsIgnoreCase(address.getType()))
                .findFirst()
                //I'm here only for testing against local cluster
                .or(() -> addresses.stream()
                        .filter(address -> "InternalIP".equalsIgnoreCase(address.getType()))
                        .findFirst())
                .map(NodeAddress::getAddress)
                .orElseThrow();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
