package com.cloudferro.copernicus.experiments.service;

import com.cloudferro.copernicus.experiments.client.data.DockerContainerDescriptor;
import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClientFactory;
import com.cloudferro.copernicus.experiments.client.kubernetes.KubernetesClusterClient;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collection;

import static com.cloudferro.copernicus.experiments.Solutions.SIMPLE_SOLUTION_ID;
import static com.cloudferro.copernicus.experiments.Solutions.SIMPLE_SOLUTION_REV;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@WireMockTest(httpPort = 8090)
class SolutionDeploymentServiceIntegrationTest {

    @MockBean
    private KubernetesClientFactory clientFactory;

    @Mock
    private KubernetesClusterClient client;

    @Autowired
    private SolutionDeploymentService deploymentService;

    @Captor
    private ArgumentCaptor<Collection<DockerContainerDescriptor>> containerDescriptorsCaptor;

    @Test
    void shouldDeploySimpleSolution_asSingleContainer() {
        //given
        var dummyConfig = "dummy";
        when(clientFactory.forKubeConfig(dummyConfig)).thenReturn(client);

        //when
        deploymentService.deploySolution(SIMPLE_SOLUTION_ID, SIMPLE_SOLUTION_REV, dummyConfig, "test");

        //then
        verify(client).deployToNamespace(containerDescriptorsCaptor.capture(), anyString());
        var descriptors = containerDescriptorsCaptor.getValue();
        assertThat(descriptors).extracting(DockerContainerDescriptor::name)
                                .containsExactly("entityrecognizer");
        assertThat(descriptors).extracting(DockerContainerDescriptor::imageUri)
                                .containsExactly("hub.cc-asp.fraunhofer.de/ai4media-public/named-entity-recognition-flair:1.0.0");
        assertThat(descriptors).extracting(DockerContainerDescriptor::protoUri)
                                .containsExactly("org/acumos/e3794e16-0225-4bf1-a99c-b99638a22232/entityrecognizer/1.0.2/entityrecognizer-1.0.2.proto");
        verify(client).waitForPods(eq("test"), any());
    }

}