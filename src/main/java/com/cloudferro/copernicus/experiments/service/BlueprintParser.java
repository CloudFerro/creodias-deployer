package com.cloudferro.copernicus.experiments.service;

import com.cloudferro.copernicus.experiments.client.data.DockerContainerDescriptor;
import com.cloudferro.copernicus.experiments.client.data.PersistentVolumeDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
public class BlueprintParser {

    @Data
    @JsonDeserialize()
    static class Blueprint {

        public static final String NODES_NODE = "nodes";
        public static final String NODES_TYPE_PROPERTY = "node_type";
        public static final String SHARED_FOLDER_NODE_TYPE = "SharedFolder";

        @Data
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        static class NodeConnection {
            private String containerName;
        }

        @Data
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        static class OperationSignature {
            private List<NodeConnection> connectedTo;
        }

        @Data
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        static class BlueprintNode {
            private String image;
            private String containerName;
            private String nodeType;
            private String protoUri;
            private List<OperationSignature> operationSignatureList;
        }

        Set<BlueprintNode> nodes;
    }

    private final String blueprintJson;
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public BlueprintParser(String blueprintJson) {
        this.blueprintJson = blueprintJson;
    }


    public List<DockerContainerDescriptor> getSolutionContainers() {
        try {
            var containers = new ArrayList<DockerContainerDescriptor>();
            var blueprintObject = mapper.readValue(blueprintJson, Blueprint.class);
            var pvs = blueprintObject.nodes
                    .stream()
                    .filter(node -> Blueprint.SHARED_FOLDER_NODE_TYPE.equalsIgnoreCase(node.getNodeType()))
                    .toList();
            blueprintObject.nodes
                    .stream().filter(node -> !Blueprint.SHARED_FOLDER_NODE_TYPE.equalsIgnoreCase(node.getNodeType()))
                    .forEach(node -> containers.add(new DockerContainerDescriptor(node.containerName, node.image, node.protoUri, checkPv(node.containerName, pvs))));
            return containers;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private PersistentVolumeDescriptor checkPv(String containerName, List<Blueprint.BlueprintNode> pvs) {
        return pvs.stream()
                .filter(pv -> pv.getOperationSignatureList().get(0).getConnectedTo().stream().anyMatch(nodeConnection -> nodeConnection.getContainerName().equals(containerName)))
                .map(pv -> new PersistentVolumeDescriptor("pipeline", pv.containerName))
                .findFirst()
                .orElse(null);
    }


    public String fixedBlueprint() {
        try {
            var blueprint = mapper.readTree(blueprintJson);
            var nodes = blueprint.get(Blueprint.NODES_NODE);
            if (nodes instanceof ArrayNode) {
                for (var i = 0; i < nodes.size(); ) {
                    String nodeType = nodes.get(i).get(Blueprint.NODES_TYPE_PROPERTY).asText();
                    if (Blueprint.SHARED_FOLDER_NODE_TYPE.equals(nodeType)) {
                        ((ArrayNode)nodes).remove(i);
                        log.info("Removed shared folder node from blueprint");
                    } else {
                        i++;
                    }
                }
            }
            return mapper.writeValueAsString(blueprint);
        } catch (JsonProcessingException ex) {
            log.error("Unable to process blueprint JSON", ex);
        }
        return blueprintJson;
    }

}
