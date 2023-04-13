package com.cloudferro.copernicus.experiments.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Valid
@Data
@ConfigurationProperties(prefix = "common-data-service")
@Configuration
public class CommonDataServiceConfiguration {
    private String user;
    private String password;
    @NotBlank
    private String url;
}
