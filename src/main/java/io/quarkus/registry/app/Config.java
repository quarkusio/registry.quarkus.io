package io.quarkus.registry.app;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class Config {

    @Inject
    @ConfigProperty(name = "registry.catalog.url", defaultValue = "https://github.com/quarkusio/quarkus-extension-catalog")
    String catalogUrl;

    @Inject
    @ConfigProperty(name = "registry.catalog.branch", defaultValue = "improved")
    String branch;

    public String getCatalogUrl() {
        return catalogUrl;
    }

    public String getBranch() {
        return branch;
    }

    @Override public String toString() {
        return "GitConfig{" +
                "catalogUrl='" + catalogUrl + '\'' +
                ", branch='" + branch + '\'' +
                '}';
    }
}
