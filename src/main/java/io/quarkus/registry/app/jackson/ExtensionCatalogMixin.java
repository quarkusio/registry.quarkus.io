package io.quarkus.registry.app.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = io.quarkus.registry.catalog.ExtensionCatalogImpl.class)
@JsonDeserialize(as = io.quarkus.registry.catalog.ExtensionCatalogImpl.Builder.class)
public interface ExtensionCatalogMixin {
}