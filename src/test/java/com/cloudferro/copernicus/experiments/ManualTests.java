package com.cloudferro.copernicus.experiments;

import com.cloudferro.copernicus.experiments.service.SolutionDeploymentService;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.cloudferro.copernicus.experiments.Solutions.*;

@SpringBootTest
@WireMockTest(httpPort = 8090)
@Disabled("To be run against real kubernetes cluster")
public class ManualTests {

    public static final String TEST_NAMESPACE = "test";
    @Autowired
    private SolutionDeploymentService deploymentService;

    @Test
    void deployToMikroK8s() throws IOException {
        var kubeConfig = Files.readString(Paths.get("src/test/resources/mk8s.config"));
        deploymentService.deploySolution(SIMPLE_SOLUTION_ID, SIMPLE_SOLUTION_REV, kubeConfig, TEST_NAMESPACE);
    }

    @Test
    void deploySudokuToMikroK8s() throws IOException {
        var kubeConfig = Files.readString(Paths.get("src/test/resources/mk8s.config"));
        deploymentService.deploySolution(COMPOSITE_SOLUTION_ID, COMPOSITE_SOLUTION_REV, kubeConfig, TEST_NAMESPACE);
    }

    @Test
    void deploySudokuToCreodias() throws IOException {
        var kubeConfig = Files.readString(Paths.get("src/test/resources/magnum.config"));
        deploymentService.deploySolution(COMPOSITE_SOLUTION_ID, COMPOSITE_SOLUTION_REV, kubeConfig, TEST_NAMESPACE);
    }

    @Test
    void deploySharedFolderToCreodias() throws IOException {
        var kubeConfig = Files.readString(Paths.get("src/test/resources/mk8s.config"));
        deploymentService.deploySolution(SHARED_FOLDER_SOLUTION_ID, SHARED_FOLDER_SOLUTION_REV, kubeConfig, TEST_NAMESPACE);
    }

}
