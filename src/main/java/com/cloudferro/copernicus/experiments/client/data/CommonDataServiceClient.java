package com.cloudferro.copernicus.experiments.client.data;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.acumos.cds.domain.MLPArtifact;
import org.acumos.cds.domain.MLPSolution;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class CommonDataServiceClient {

    public static final String SOLUTION_PATH = "/solution/{solutionId}";
    public static final String ARTIFACT_PATH = "/revision/{revisionId}/artifact";

    private final RestTemplate restTemplate;

    public SolutionDescriptor getSolution(String solutionId) {
        var mlpSolution = restTemplate.getForEntity(SOLUTION_PATH, MLPSolution.class, solutionId).getBody();

        if (mlpSolution == null) {
            throw new RuntimeException("Solution not returned for ID " + solutionId);
        }

        var solutionType = Optional.ofNullable(mlpSolution.getToolkitTypeCode())
                .filter("CP"::equals)
                .map(typeCode -> SolutionType.COMPOSITE)
                .orElse(SolutionType.SIMPLE);


        return new SolutionDescriptor(mlpSolution.getName(), solutionType);
    }

    public DockerContainerDescriptor getContainerDescriptor(String revisionId) {
        var mlpArtifacts = restTemplate.getForEntity(ARTIFACT_PATH, MLPArtifact[].class, revisionId).getBody();
        if (mlpArtifacts == null) {
            throw new RuntimeException("No artifacts returned for revision " + revisionId);
        }

        String proto = null;
        String name = null;
        String image = null;
        for (MLPArtifact artifact : mlpArtifacts) {
            if (artifact.getArtifactTypeCode() != null && artifact.getArtifactTypeCode().equalsIgnoreCase("DI")) {
                name = artifact.getName();
                image = artifact.getUri();
            }
            if (artifact.getArtifactTypeCode() != null && artifact.getArtifactTypeCode().equalsIgnoreCase("MI")) {
                proto = artifact.getUri();
            }
            if (name != null && image != null && proto != null) {
                return new DockerContainerDescriptor(name, image, proto, null);
            }
        }

        throw new RuntimeException("No DI type artifacts returned for revision " + revisionId);
    }

    public String getBlueprintUri(String revisionId) {
        var mlpArtifacts = restTemplate.getForEntity(ARTIFACT_PATH, MLPArtifact[].class, revisionId).getBody();
        if (mlpArtifacts == null) {
            throw new RuntimeException("No artifacts returned for revision " + revisionId);
        }

        for (MLPArtifact artifact : mlpArtifacts) {
            if (artifact.getArtifactTypeCode() != null && artifact.getArtifactTypeCode().equalsIgnoreCase("BP")) {
                return artifact.getUri();
            }
        }

        throw new RuntimeException("No BP type artifacts returned for revision " + revisionId);
    }

}
