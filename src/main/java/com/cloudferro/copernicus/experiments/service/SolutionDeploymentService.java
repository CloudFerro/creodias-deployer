package com.cloudferro.copernicus.experiments.service;

import ai4eu.grpc.OrchestratorGrpc;
import ai4eu.grpc.OrchestratorOuterClass;
import com.cloudferro.copernicus.experiments.client.data.CommonDataServiceClient;
import com.cloudferro.copernicus.experiments.client.data.DockerContainerDescriptor;
import com.cloudferro.copernicus.experiments.client.data.SolutionType;
import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClientFactory;
import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClusterClient;
import com.cloudferro.copernicus.experiments.client.nexus.NexusClient;
import com.cloudferro.copernicus.experiments.config.OrchestratorConfiguration;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SolutionDeploymentService {

    private final CommonDataServiceClient dataClient;
    private final NexusClient nexusClient;
    private final OrchestratorConfiguration orchestratorConfiguration;
    private final KubernetesClientFactory kubernetesClientFactory;
    private final Duration waitTimeout;

    public SolutionDeploymentService(CommonDataServiceClient dataClient, NexusClient nexusClient,
                                     OrchestratorConfiguration orchestratorConfiguration,
                                     KubernetesClientFactory kubernetesClientFactory,
                                     @Value("${deployer.podsReadyTimeout:60s}") Duration waitTimeout) {
        this.dataClient = dataClient;
        this.nexusClient = nexusClient;
        this.orchestratorConfiguration = orchestratorConfiguration;
        this.kubernetesClientFactory = kubernetesClientFactory;
        this.waitTimeout = waitTimeout;
    }

    public String getDefaultNamespaceName(String solutionId, String revisionId) {
        var solution = dataClient.getSolution(solutionId);
        return getNamespaceName(solution.name(), revisionId);
    }

    public Set<KubernetesClusterClient.ServiceAddress> deploySolution(@NonNull String solutionId, @NonNull String revisionId, @NonNull String kubeConfig, @NonNull String namespace) {
        var solution = dataClient.getSolution(solutionId);
        try (var k8sClient = kubernetesClientFactory.forKubeConfig(kubeConfig)) {
            if (SolutionType.SIMPLE == solution.solutionType()) {
                log.info("Deploying simple solution {} to namespace {}", solution, namespace);
                k8sClient.deployToNamespace(List.of(dataClient.getContainerDescriptor(revisionId)), namespace);
                k8sClient.waitForPods(namespace, waitTimeout);
                return k8sClient.getServices(namespace);
            } else {
                log.info("Processing composite solution {}", solution);
                var blueprintArtifact = dataClient.getBlueprintUri(revisionId);
                return nexusClient.getArtifact(blueprintArtifact)
                        .map(blueprintJson -> {
                            log.info("Blueprint found for solution {}, deploying to namespace {}", solution, namespace);
                            var blueprintParser = new BlueprintParser(blueprintJson);
                            var containers = blueprintParser.getSolutionContainers();
                            k8sClient.deployToNamespace(containers, namespace);
                            k8sClient.deployToNamespace(List.of(new DockerContainerDescriptor(orchestratorConfiguration.getName(),
                                    orchestratorConfiguration.getImageName(), "", null)), namespace);
                            k8sClient.waitForPods(namespace, waitTimeout);
                            var protos = getProtobufSpecs(containers);
                            return runSolution(k8sClient, namespace, blueprintParser.fixedBlueprint(), protos);
                        })
                        .orElseThrow(() -> new RuntimeException("Failed to retrieve blueprint for revision " + revisionId));
            }
        }
    }

    private String getNamespaceName(String solutionName, String revisionId) {
        return String.format("%s-%s", solutionName, revisionId).toLowerCase().replace(" ", "-");
    }

    private Map<String, String> getProtobufSpecs(List<DockerContainerDescriptor> containers) {
        return containers.stream()
                .collect(Collectors.toMap(
                        container -> container.name() + ".proto",
                        container -> nexusClient.getArtifact(container.protoUri()).orElse("")));
    }

    private Set<KubernetesClusterClient.ServiceAddress> runSolution(KubernetesClusterClient k8sClient, String namespaceName, String blueprint, Map<String, String> protos) {
        var services = k8sClient.getServices(namespaceName);
        var dockerInfo = DockerInfoBuilder.dockerInfoJson(services);
        var orchestratorService = services.stream()
                .filter(service -> service.name().equals(orchestratorConfiguration.getName()))
                .findFirst()
                .orElseThrow();
        runOrchestrator(orchestratorService.host(), orchestratorService.protobufPort().external(), blueprint, dockerInfo, protos);
        return services;
    }

    private void runOrchestrator(String firstNodeIp, Integer nodePort, String blueprint, String dockerInfoJson, Map<String, String> protos) {
        var channel = ManagedChannelBuilder.forAddress(firstNodeIp, nodePort).usePlaintext().build();
        var stub = OrchestratorGrpc.newBlockingStub(channel);
        var request = OrchestratorOuterClass.OrchestrationConfiguration.newBuilder()
                .setBlueprint(blueprint)
                .setDockerinfo(dockerInfoJson)
                .putAllProtofiles(protos)
                .setQueuesize(0)
                .setIterations(0)
                .build();

        var emptyLabel = OrchestratorOuterClass.RunLabel.newBuilder().build();
        var status = stub.getStatus(emptyLabel);
        if ("not initialized".equals(status.getMessage())) {
            var response = stub.initialize(request);
            if (!"initialized".equals(response.getMessage())) {
                throw new RuntimeException("Failed to initialize orchestrator. Status: " + response.getMessage());
            }
            stub.run(emptyLabel);
        }
    }



}
