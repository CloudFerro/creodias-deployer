package com.cloudferro.copernicus.experiments.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@Data
@ConfigurationProperties(prefix = "blueprint")
@Configuration
public class OrchestratorConfiguration {
    @NotBlank
    private String name;
    @NotBlank
    private String imageName;
}
