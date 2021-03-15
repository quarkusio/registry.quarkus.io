package io.quarkus.registry.app.jackson;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.registry.catalog.json.JsonCatalogMapperHelper;

/**
 * Apply the patterns used in this project
 */
@ApplicationScoped
public class AppObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        JsonCatalogMapperHelper.initMapper(objectMapper);
    }

}
