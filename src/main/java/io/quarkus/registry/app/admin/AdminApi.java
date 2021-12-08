package io.quarkus.registry.app.admin;

import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.jaxrs.yaml.YAMLMediaTypes;
import io.quarkus.logging.Log;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.app.events.ExtensionCatalogImportEvent;
import io.quarkus.registry.app.events.ExtensionCompatibilityCreateEvent;
import io.quarkus.registry.app.events.ExtensionCompatibleDeleteEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.ExtensionDeleteEvent;
import io.quarkus.registry.app.maven.cache.MavenCache;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.catalog.json.JsonExtension;
import io.quarkus.registry.catalog.json.JsonExtensionCatalog;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import static org.apache.commons.lang3.StringUtils.abbreviate;

@ApplicationScoped
@Path("/admin")
@RolesAllowed("admin")
@SecurityScheme(securitySchemeName = "Authentication",
        description = "Admin token",
        type = SecuritySchemeType.APIKEY,
        apiKeyName = "TOKEN",
        in = SecuritySchemeIn.HEADER)
@Tag(name = "Admin", description = "Admin related services")
public class AdminApi {

    private static final int MAX_ABBREVIATION_WIDTH = 100;
    @Inject
    AdminService adminService;

    @Inject MavenCache cache;

    @POST
    @Path("/v1/extension/catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @SecurityRequirement(name = "Authentication")
    public Response addExtensionCatalog(
            @NotNull(message = "X-Platform header missing") @HeaderParam("X-Platform") String platformKey,
            @DefaultValue("false") @HeaderParam("X-Platform-Pinned") boolean pinned,
            @NotNull(message = "Body payload is missing") JsonExtensionCatalog catalog) {
        ArtifactCoords bom = catalog.getBom();
        Platform platform = Platform.findByKey(platformKey)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        Optional<PlatformRelease> platformRelease = PlatformRelease.findByPlatformKey(platformKey, bom.getVersion());
        if (platformRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        Log.infof("Adding catalog %s", abbreviate(catalog.toString(), MAX_ABBREVIATION_WIDTH));
        ExtensionCatalogImportEvent event = new ExtensionCatalogImportEvent(platform, catalog, pinned);
        adminService.onExtensionCatalogImport(event);
        return Response.accepted(bom).build();
    }

    @POST
    @Path("/v1/extension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @SecurityRequirement(name = "Authentication")
    public Response addExtension(@NotNull(message = "Body payload is missing") JsonExtension extension) {
        ArtifactCoords bom = extension.getArtifact();
        Optional<ExtensionRelease> extensionRelease = ExtensionRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (extensionRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        Log.infof("Adding extension %s", abbreviate(extension.toString(), MAX_ABBREVIATION_WIDTH));
        ExtensionCreateEvent event = new ExtensionCreateEvent(extension);
        adminService.onExtensionCreate(event);
        return Response.accepted(bom).build();
    }

    @DELETE
    @Path("/v1/extension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @SecurityRequirement(name = "Authentication")
    public Response deleteExtension(@NotNull(message = "groupId is missing") @FormParam("groupId") String groupId,
            @NotNull(message = "artifactId is missing") @FormParam("artifactId") String artifactId) {
        Extension extension = Extension.findByGA(groupId, artifactId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        Log.infof("Removing extension %s:%s", abbreviate(groupId, MAX_ABBREVIATION_WIDTH),
                abbreviate(artifactId, MAX_ABBREVIATION_WIDTH));
        ExtensionDeleteEvent event = new ExtensionDeleteEvent(extension);
        adminService.onExtensionDelete(event);
        return Response.accepted(new ArtifactKey(groupId, artifactId)).build();
    }

    @POST
    @Path("/v1/extension/compat")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @SecurityRequirement(name = "Authentication")
    public Response addExtensionCompatibilty(
            @NotNull(message = "groupId is missing") @FormParam("groupId") String groupId,
            @NotNull(message = "artifactId is missing") @FormParam("artifactId") String artifactId,
            @NotNull(message = "version is missing") @FormParam("version") String version,
            @NotNull(message = "quarkusCore is missing") @FormParam("quarkusCore") String quarkusCore,
            @DefaultValue("true") @FormParam("compatible") Boolean compatible) {
        ExtensionRelease extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        Log.infof("Extension %s:%s:%s is %s with Quarkus %s",
                abbreviate(groupId, MAX_ABBREVIATION_WIDTH),
                abbreviate(artifactId, MAX_ABBREVIATION_WIDTH),
                abbreviate(version, MAX_ABBREVIATION_WIDTH),
                compatible ? "compatible" : "incompatible",
                abbreviate(quarkusCore, MAX_ABBREVIATION_WIDTH));
        ExtensionCompatibilityCreateEvent event = new ExtensionCompatibilityCreateEvent(extensionRelease, quarkusCore,
                compatible);
        adminService.onExtensionCompatibilityCreate(event);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/v1/extension/compat")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @SecurityRequirement(name = "Authentication")
    public Response removeExtensionCompatibilty(@NotNull(message = "groupId is missing") @FormParam("groupId") String groupId,
            @NotNull(message = "artifactId is missing") @FormParam("artifactId") String artifactId,
            @NotNull(message = "version is missing") @FormParam("version") String version,
            @NotNull(message = "quarkusCore is missing") @FormParam("quarkusCore") String quarkusCore) {
        ExtensionRelease extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, version)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        Log.infof("Extension %s:%s:%s is no longer compatible with Quarkus %s",
                abbreviate(groupId, MAX_ABBREVIATION_WIDTH),
                abbreviate(artifactId, MAX_ABBREVIATION_WIDTH),
                abbreviate(version, MAX_ABBREVIATION_WIDTH),
                abbreviate(quarkusCore, MAX_ABBREVIATION_WIDTH));
        ExtensionCompatibleDeleteEvent event = new ExtensionCompatibleDeleteEvent(extensionRelease, quarkusCore);
        adminService.onExtensionCompatibilityDelete(event);
        return Response.accepted().build();
    }

    @DELETE
    @Path("/v1/maven/cache")
    @SecurityRequirement(name = "Authentication")
    public Response clearCache() {
        cache.clear();
        return Response.accepted().build();
    }
}
