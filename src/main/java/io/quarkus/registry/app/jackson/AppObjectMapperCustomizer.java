package io.quarkus.registry.app.jackson;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.registry.catalog.CatalogMapperHelper;

/**
 * Apply the patterns used in this project
 */
@ApplicationScoped
public class AppObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        CatalogMapperHelper.initMapper(objectMapper);
    }

}
