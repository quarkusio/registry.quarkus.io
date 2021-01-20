package io.quarkus.registry.app.services;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.registry.app.model.Extension;
import io.smallrye.mutiny.Uni;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.VersionRangeResult;

@ApplicationScoped
public class RegistryService {

    @Inject
    MavenArtifactResolver resolver;

    @Transactional
    public Uni<Extension> includeExtension(String groupId, String artifactId) {
        Extension extension = new Extension();
        extension.groupId = groupId;
        extension.artifactId = artifactId;
        extension.name = "An extension";
        extension.description = "An extension description";
        return extension.persistAndFlush().onItem().castTo(Extension.class);
    }

    private String resolveLatestVersion(String groupId, String artifactId) throws BootstrapMavenException {
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, null, null);
        VersionRangeResult versionRange = resolver.resolveVersionRange(artifact);
        return versionRange.getHighestVersion().toString();
    }
}
