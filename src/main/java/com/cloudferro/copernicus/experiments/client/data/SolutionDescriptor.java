package com.cloudferro.copernicus.experiments.client.data;

import org.springframework.lang.NonNull;

public record SolutionDescriptor(
        @NonNull String name,
        @NonNull SolutionType solutionType) {}