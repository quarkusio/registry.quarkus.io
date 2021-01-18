package io.quarkus.registry.app.jobs;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.registry.app.Config;
import io.quarkus.runtime.Startup;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRangeResult;

/**
 * Index the catalog
 */
@Startup
public class IndexCatalogJob {

    @Inject
    Config config;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    MavenArtifactResolver resolver;

    @PostConstruct
    public void index() {
        //        Path catalog = null;
        //        try {
        //            catalog = Files.createTempDirectory("catalog");
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }
        //        System.out.println("CATALOG : " + catalog);
        //        try (Git git = Git.cloneRepository().setDirectory(catalog.toFile())
        //                .setURI(config.getCatalogUrl())
        //                .setBranch(config.getBranch())
        //                .call()) {
        //
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }

        DefaultArtifact artifact = new DefaultArtifact("io.quarkus", "quarkus-bom", "pom", "[,)");
        try {
            VersionRangeResult versionRange = resolver.resolveVersionRange(artifact);
            System.out.println(versionRange.getVersions());
        } catch (BootstrapMavenException e) {
            e.printStackTrace();
        }
    }
}
