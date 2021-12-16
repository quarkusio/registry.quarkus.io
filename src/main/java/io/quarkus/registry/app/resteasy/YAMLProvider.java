package io.quarkus.registry.app.resteasy;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.jaxrs.yaml.JacksonYAMLProvider;
import com.fasterxml.jackson.jaxrs.yaml.YAMLMediaTypes;

import io.quarkus.registry.app.jackson.ExtensionCatalogMixin;
import io.quarkus.registry.app.jackson.ExtensionMixin;
import io.quarkus.registry.catalog.CatalogMapperHelper;

/**
 * This initializes the JacksonYAMLProvider with my custom YAMLMapper.
 */
@Provider
@Consumes(YAMLMediaTypes.APPLICATION_JACKSON_YAML)
@Produces(YAMLMediaTypes.APPLICATION_JACKSON_YAML)
public class YAMLProvider extends JacksonYAMLProvider {

    private static final YAMLMapper mapper = new YAMLMapper();

    static {
        CatalogMapperHelper.initMapper(mapper);
        mapper.addMixIn(io.quarkus.registry.catalog.ExtensionCatalog.class, ExtensionCatalogMixin.class);
        mapper.addMixIn(io.quarkus.registry.catalog.Extension.class, ExtensionMixin.class);
    }

    public YAMLProvider() {
        super(mapper);
    }
}
