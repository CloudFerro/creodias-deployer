package com.cloudferro.copernicus.experiments.service;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DockerInfo(List<DockerInfoItem> dockerInfoList) {
}
