package com.cloudferro.copernicus.experiments.client.data;

import com.cloudferro.copernicus.experiments.Solutions;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@WireMockTest(httpPort = 8090)
class CommonDataServiceClientIntegrationTest {

    @Autowired
    private CommonDataServiceClient commonDataServiceClient;

    @Test
    void shouldRecogniseSimpleSolution() {
        var solution = commonDataServiceClient.getSolution(Solutions.SIMPLE_SOLUTION_ID);
        assertEquals(SolutionType.SIMPLE, solution.solutionType());
        assertEquals("EntityRecognizer", solution.name());
    }

    @Test
    void shouldReturnContainerDescriptorForSimpleSolution() {
        var descriptor = commonDataServiceClient.getContainerDescriptor(Solutions.SIMPLE_SOLUTION_REV);
        assertEquals("entityrecognizer", descriptor.name());
        assertEquals("hub.cc-asp.fraunhofer.de/ai4media-public/named-entity-recognition-flair:1.0.0", descriptor.imageUri());
        assertEquals("org/acumos/e3794e16-0225-4bf1-a99c-b99638a22232/entityrecognizer/1.0.2/entityrecognizer-1.0.2.proto", descriptor.protoUri());
        assertNull(descriptor.pv());
    }

    @Test
    void shouldRecogniseCompositeSolution() {
        var solution = commonDataServiceClient.getSolution(Solutions.COMPOSITE_SOLUTION_ID);
        assertEquals(SolutionType.COMPOSITE, solution.solutionType());
        assertEquals("Sudoku Tutorial", solution.name());
    }

    @Test
    void shouldReturnBlueprintUriForCompositeSolution() {
        assertEquals("org/acumos/00aff3ab-94cb-4969-93c3-a95be53c05d2/0ef0dba6-7541-48ca-bd83-c14a7a1b1308/BLUEPRINT-00AFF3AB-94CB-4969-93C3-A95BE53C05D2/2/BLUEPRINT-00AFF3AB-94CB-4969-93C3-A95BE53C05D2-2.json", commonDataServiceClient.getBlueprintUri(Solutions.COMPOSITE_SOLUTION_REV));
    }
}