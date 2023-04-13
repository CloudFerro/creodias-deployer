package com.cloudferro.copernicus.experiments.client.data;

import org.springframework.lang.NonNull;

public record DockerContainerDescriptor(
        @NonNull String name,
        @NonNull String imageUri,
        @NonNull String protoUri,
        PersistentVolumeDescriptor pv
) {}