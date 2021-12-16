package io.quarkus.registry.app.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = io.quarkus.registry.catalog.ExtensionImpl.class)
@JsonDeserialize(as = io.quarkus.registry.catalog.ExtensionImpl.Builder.class)
public interface ExtensionMixin {
}
