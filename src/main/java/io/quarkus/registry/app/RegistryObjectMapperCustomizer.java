package io.quarkus.registry.app;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.quarkus.jackson.ObjectMapperCustomizer;

/**
 * Apply the patterns used in this project
 */
@ApplicationScoped
public class RegistryObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);
    }

}
