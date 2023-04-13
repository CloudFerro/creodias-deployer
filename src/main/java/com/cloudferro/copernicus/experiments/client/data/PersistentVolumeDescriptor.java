package com.cloudferro.copernicus.experiments.client.data;

import org.springframework.lang.NonNull;

public record PersistentVolumeDescriptor(
        @NonNull String name,
        @NonNull String path

) {}