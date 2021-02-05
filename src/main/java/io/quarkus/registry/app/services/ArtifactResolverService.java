package io.quarkus.registry.app.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fabric8.maven.Maven;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.resolver.json.QuarkusJsonPlatformDescriptorResolver;
import io.smallrye.common.io.jar.JarFiles;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.VersionRangeResult;

@ApplicationScoped
public class ArtifactResolverService {

    private final MavenArtifactResolver resolver;
    private final ObjectMapper objectMapperYaml;
    private final QuarkusJsonPlatformDescriptorResolver descriptorResolver;

    @Inject
    public ArtifactResolverService(MavenArtifactResolver resolver) {
        this.resolver = resolver;
        this.objectMapperYaml = new ObjectMapper(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        descriptorResolver = QuarkusJsonPlatformDescriptorResolver.newInstance()
                .setArtifactResolver(new BootstrapAppModelResolver(resolver));
    }

    public QuarkusPlatformDescriptor resolvePlatformDescriptor(String groupId, String artifactId, String version) {
        return descriptorResolver.resolveFromBom(groupId, artifactId, version);
    }

    public JsonNode readExtensionYaml(String groupId, String artifactId, String version) {
        try {
            // Extract the YAML from the JAR
            ArtifactResult result = resolver.resolve(new DefaultArtifact(groupId, artifactId, "jar", version));
            try (JarFile jarFile = JarFiles.create(result.getArtifact().getFile())) {
                ZipEntry yamlEntry = jarFile.getEntry("META-INF/quarkus-extension.yaml");
                try (InputStream is = jarFile.getInputStream(yamlEntry)) {
                    return objectMapperYaml.readTree(is);
                }
            }
        } catch (BootstrapMavenException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Model resolveLatestModel(String groupId, String artifactId) {
        String latestVersion = resolveLatestVersion(groupId, artifactId);
        return resolveModel(groupId, artifactId, latestVersion);
    }

    public Model resolveModel(String groupId, String artifactId, String version) {
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, "pom", version);
        ArtifactResult result;
        try {
            result = resolver.resolve(artifact);
        } catch (BootstrapMavenException e) {
            String message = String.format("No descriptor could be found for %s:%s:%s", groupId, artifactId, version);
            throw new IllegalStateException(message, e);
        }
        return Maven.readModel(result.getArtifact().getFile().toPath());
    }

    public String resolveLatestVersion(String groupId, String artifactId) {
        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, null, "(,]");
        VersionRangeResult versionRange = null;
        try {
            versionRange = resolver.resolveVersionRange(artifact);
        } catch (BootstrapMavenException e) {
            throw new IllegalArgumentException("No version range found for " + groupId + ":" + artifactId);
        }
        return versionRange.getHighestVersion().toString();
    }
}
