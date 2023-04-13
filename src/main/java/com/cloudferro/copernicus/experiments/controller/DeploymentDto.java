package com.cloudferro.copernicus.experiments.controller;

import lombok.Data;

@Data
public class DeploymentDto {
    private String solutionId;
    private String revisionId;
    private String namespaceName;
    private String config;
}
