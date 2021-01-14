package io.quarkus.registry.app.jobs;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.registry.app.Config;
//import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.registry.catalog.model.Repository;
import io.quarkus.runtime.Startup;
import org.eclipse.jgit.api.Git;

/**
 * Index the catalog
 */
@Startup
public class IndexCatalogJob {

    @Inject
    Config config;

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    public void index() {
        Path catalog = null;
        try {
            catalog = Files.createTempDirectory("catalog");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("CATALOG : " + catalog);
        try (Git git = Git.cloneRepository().setDirectory(catalog.toFile())
                .setURI(config.getCatalogUrl())
                .setBranch(config.getBranch())
                .call()) {

        } catch (Exception e) {
            e.printStackTrace();
        }

        Repository repository = Repository.parse(catalog, objectMapper);

        System.out.println(repository.getPlatforms());
        System.out.println(repository.getIndividualExtensions());

    }
}
