package com.cloudferro.copernicus.experiments.service;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DockerInfoItem(String containerName, String ipAddress, Integer port) {

}
