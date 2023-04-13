package com.cloudferro.copernicus.experiments.service;

import com.cloudferro.copernicus.experiments.client.data.PersistentVolumeDescriptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlueprintParserTest {

    public static final String SHARED_FOLDER_BP = """
            {
                  "name": "news-training-pi",
                  "version": "1.0.0",
                  "input_ports": [],
                  "nodes": [
                    {
                      "container_name": "/data/shared",
                      "node_type": "SharedFolder",
                      "image": "cicd.ai4eu-dev.eu:7444/virtual:v1",
                      "proto_uri": "org/acumos/152894f9-853e-45fc-8879-7bfcb852c7a7/sharedfolderprovider/1.0.0/sharedfolderprovider-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "provideFolder",
                            "input_message_name": "Empty",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderRequest",
                            "output_message_stream": false
                          },
                          "connected_to": [
                            {
                              "container_name": "tensorboard1",
                              "operation_signature": {
                                "operation_name": "mountVolume"
                              }
                            },
                            {
                              "container_name": "newstrainer1",
                              "operation_signature": {
                                "operation_name": "mountVolume"
                              }
                            },
                            {
                              "container_name": "news-classifier1",
                              "operation_signature": {
                                "operation_name": "mountVolume"
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "container_name": "tensorboard1",
                      "node_type": "MLModel",
                      "image": "cicd.ai4eu-dev.eu:7444/training_pipeline/tensorboard:v1",
                      "proto_uri": "org/acumos/99a477a3-99ef-4ca9-a1e2-b21d520d554f/tensorboard/1.0.0/tensorboard-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "mountVolume",
                            "input_message_name": "SharedFolderRequest",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        }
                      ]
                    },
                    {
                      "container_name": "newstrainer1",
                      "node_type": "MLModel",
                      "image": "cicd.ai4eu-dev.eu:7444/training_pipeline/news_trainer:v1",
                      "proto_uri": "org/acumos/a3aa7df6-14d1-477f-ae22-de47374251ac/newstrainer/1.0.0/newstrainer-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "mountVolume",
                            "input_message_name": "SharedFolderRequest",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        },
                        {
                          "operation_signature": {
                            "operation_name": "startTraining",
                            "input_message_name": "Empty",
                            "input_message_stream": false,
                            "output_message_name": "TrainingConfig",
                            "output_message_stream": false
                          },
                          "connected_to": [
                            {
                              "container_name": "news-classifier1",
                              "operation_signature": {
                                "operation_name": "startTraining"
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "container_name": "news-classifier1",
                      "node_type": "MLModel",
                      "image": "cicd.ai4eu-dev.eu:7444/training_pipeline/news_classifier:v1",
                      "proto_uri": "org/acumos/adba1c6e-8c95-4eaf-acfa-7b9827cf42d6/newsclassifier/1.0.0/newsclassifier-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "mountVolume",
                            "input_message_name": "SharedFolderRequest",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        },
                        {
                          "operation_signature": {
                            "operation_name": "startTraining",
                            "input_message_name": "TrainingConfig",
                            "input_message_stream": false,
                            "output_message_name": "TrainingStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        }
                      ]
                    }
                  ],
                  "probeIndicator": [
                    {
                      "value": "false"
                    }
                  ]
                }
            """;

    @Test
    void shouldReturnContainersFromBlueprint() {
        var parser = new BlueprintParser(SHARED_FOLDER_BP);
        var containers = parser.getSolutionContainers();
        assertEquals(3, containers.size());
        assertEquals("tensorboard1", containers.get(0).name());
        assertEquals("cicd.ai4eu-dev.eu:7444/training_pipeline/tensorboard:v1", containers.get(0).imageUri());
        assertEquals("org/acumos/99a477a3-99ef-4ca9-a1e2-b21d520d554f/tensorboard/1.0.0/tensorboard-1.0.0.proto", containers.get(0).protoUri());
        assertEquals(new PersistentVolumeDescriptor("pipeline", "/data/shared"), containers.get(0).pv());

        assertEquals("news-classifier1", containers.get(1).name());
        assertEquals("cicd.ai4eu-dev.eu:7444/training_pipeline/news_classifier:v1", containers.get(1).imageUri());
        assertEquals("org/acumos/adba1c6e-8c95-4eaf-acfa-7b9827cf42d6/newsclassifier/1.0.0/newsclassifier-1.0.0.proto", containers.get(1).protoUri());
        assertEquals(new PersistentVolumeDescriptor("pipeline", "/data/shared"), containers.get(1).pv());

        assertEquals("newstrainer1", containers.get(2).name());
        assertEquals("cicd.ai4eu-dev.eu:7444/training_pipeline/news_trainer:v1", containers.get(2).imageUri());
        assertEquals("org/acumos/a3aa7df6-14d1-477f-ae22-de47374251ac/newstrainer/1.0.0/newstrainer-1.0.0.proto", containers.get(2).protoUri());
        assertEquals(new PersistentVolumeDescriptor("pipeline", "/data/shared"), containers.get(2).pv());
    }

    @Test
    void shouldRemoveSharedFolderNode() {
        var fixedBp = """
                {
                  "name": "news-training-pi",
                  "version": "1.0.0",
                  "input_ports": [],
                  "nodes": [
                    {
                      "container_name": "tensorboard1",
                      "node_type": "MLModel",
                      "image": "cicd.ai4eu-dev.eu:7444/training_pipeline/tensorboard:v1",
                      "proto_uri": "org/acumos/99a477a3-99ef-4ca9-a1e2-b21d520d554f/tensorboard/1.0.0/tensorboard-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "mountVolume",
                            "input_message_name": "SharedFolderRequest",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        }
                      ]
                    },
                    {
                      "container_name": "newstrainer1",
                      "node_type": "MLModel",
                      "image": "cicd.ai4eu-dev.eu:7444/training_pipeline/news_trainer:v1",
                      "proto_uri": "org/acumos/a3aa7df6-14d1-477f-ae22-de47374251ac/newstrainer/1.0.0/newstrainer-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "mountVolume",
                            "input_message_name": "SharedFolderRequest",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        },
                        {
                          "operation_signature": {
                            "operation_name": "startTraining",
                            "input_message_name": "Empty",
                            "input_message_stream": false,
                            "output_message_name": "TrainingConfig",
                            "output_message_stream": false
                          },
                          "connected_to": [
                            {
                              "container_name": "news-classifier1",
                              "operation_signature": {
                                "operation_name": "startTraining"
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "container_name": "news-classifier1",
                      "node_type": "MLModel",
                      "image": "cicd.ai4eu-dev.eu:7444/training_pipeline/news_classifier:v1",
                      "proto_uri": "org/acumos/adba1c6e-8c95-4eaf-acfa-7b9827cf42d6/newsclassifier/1.0.0/newsclassifier-1.0.0.proto",
                      "operation_signature_list": [
                        {
                          "operation_signature": {
                            "operation_name": "mountVolume",
                            "input_message_name": "SharedFolderRequest",
                            "input_message_stream": false,
                            "output_message_name": "SharedFolderStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        },
                        {
                          "operation_signature": {
                            "operation_name": "startTraining",
                            "input_message_name": "TrainingConfig",
                            "input_message_stream": false,
                            "output_message_name": "TrainingStatus",
                            "output_message_stream": false
                          },
                          "connected_to": []
                        }
                      ]
                    }
                  ],
                  "probeIndicator": [
                    {
                      "value": "false"
                    }
                  ]
                }
                """.replaceAll("\\s", "");

        var parser = new BlueprintParser(SHARED_FOLDER_BP);
        assertEquals(fixedBp, parser.fixedBlueprint());
    }
}