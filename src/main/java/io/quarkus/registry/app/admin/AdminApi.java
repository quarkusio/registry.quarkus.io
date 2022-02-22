package io.quarkus.registry.app.admin;

import static org.apache.commons.lang3.StringUtils.abbreviate;

import java.util.Optional;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.jaxrs.yaml.YAMLMediaTypes;

import io.quarkus.logging.Log;
import io.quarkus.maven.ArtifactCoords;
import io.quarkus.maven.ArtifactKey;
import io.quarkus.registry.app.events.ExtensionCatalogImportEvent;
import io.quarkus.registry.app.events.ExtensionCompatibilityCreateEvent;
import io.quarkus.registry.app.events.ExtensionCompatibleDeleteEvent;
import io.quarkus.registry.app.events.ExtensionCreateEvent;
import io.quarkus.registry.app.events.ExtensionDeleteEvent;
import io.quarkus.registry.app.events.ExtensionReleaseDeleteEvent;
import io.quarkus.registry.app.maven.cache.MavenCache;
import io.quarkus.registry.app.model.DbState;
import io.quarkus.registry.app.model.Extension;
import io.quarkus.registry.app.model.ExtensionRelease;
import io.quarkus.registry.app.model.Platform;
import io.quarkus.registry.app.model.PlatformRelease;
import io.quarkus.registry.app.model.PlatformStream;
import io.quarkus.registry.catalog.ExtensionCatalog;

@ApplicationScoped
@Path("/admin")
@RolesAllowed("admin")
@SecurityScheme(securitySchemeName = "Authentication", description = "Admin token", type = SecuritySchemeType.APIKEY, apiKeyName = "TOKEN", in = SecuritySchemeIn.HEADER)
@Tag(name = "Admin", description = "Admin related services")
public class AdminApi {

    /**
     * Maximum abbreviation width for extensions
     */
    private static final int MAX_ABBREVIATION_WIDTH = 100;

    @Inject
    AdminService adminService;

    @Inject
    MavenCache cache;

    @POST
    @Path("/v1/extension/catalog")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes({ MediaType.APPLICATION_JSON, YAMLMediaTypes.APPLICATION_JACKSON_YAML })
    @SecurityRequirement(name = "Authentication")
    @Operation(summary = "Inserts a new Extension Catalog (Platform Release) in the database", description = "Invoke this endpoint when a new extension catalog release is available")
    public Response addExtensionCatalog(
            @NotNull(message = "X-Platform header missing") @HeaderParam("X-Platform") String platformKey,
            @DefaultValue("false") @HeaderParam("X-Platform-Pinned") boolean pinned,
            @NotNull(message = "Body payload is missing") ExtensionCatalog catalog) {
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
    @Operation(summary = "Inserts an extension in the database")
    public Response addExtension(
            @NotNull(message = "Body payload is missing") io.quarkus.registry.catalog.Extension extension) {
        ArtifactCoords bom = extension.getArtifact();
        Optional<ExtensionRelease> extensionRelease = ExtensionRelease
                .findByGAV(bom.getGroupId(), bom.getArtifactId(), bom.getVersion());
        if (extensionRelease.isPresent()) {
            return Response.status(Response.Status.CONFLICT).build();
        }
        Log.infof("Adding extension %s", abbreviate(String.valueOf(extension.getArtifact()), MAX_ABBREVIATION_WIDTH));
        ExtensionCreateEvent event = new ExtensionCreateEvent(extension);
        adminService.onExtensionCreate(event);
        return Response.accepted(bom).build();
    }

    @DELETE
    @Path("/v1/extension")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @SecurityRequirement(name = "Authentication")
    @Operation(summary = "Deletes an extension from the database")
    public Response deleteExtension(@NotNull(message = "groupId is missing") @FormParam("groupId") String groupId,
            @NotNull(message = "artifactId is missing") @FormParam("artifactId") String artifactId,
            @FormParam("version") String version) {
        if (version != null) {
            ArtifactCoords entity = new ArtifactCoords(groupId, artifactId, version);
            ExtensionRelease extensionRelease = ExtensionRelease.findByGAV(groupId, artifactId, version)
                    .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
            // If an extension release belongs to a platform, do not remove
            if (!extensionRelease.platforms.isEmpty()) {
                return Response.status(Response.Status.NOT_ACCEPTABLE)
                        .entity(entity)
                        .build();
            } else {
                Log.infof("Removing extension release %s:%s:s",
                        abbreviate(groupId, MAX_ABBREVIATION_WIDTH),
                        abbreviate(artifactId, MAX_ABBREVIATION_WIDTH),
                        abbreviate(version, MAX_ABBREVIATION_WIDTH));
                ExtensionReleaseDeleteEvent event = new ExtensionReleaseDeleteEvent(extensionRelease);
                adminService.onExtensionReleaseDelete(event);
                return Response.accepted(entity).build();
            }
        } else {
            ArtifactKey entity = new ArtifactKey(groupId, artifactId);
            Extension extension = Extension.findByGA(groupId, artifactId)
                    .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
            Log.infof("Removing extension %s:%s", abbreviate(groupId, MAX_ABBREVIATION_WIDTH),
                    abbreviate(artifactId, MAX_ABBREVIATION_WIDTH));
            // If any extension release belongs to a platform, do not delete it
            if (extension.releases.stream().anyMatch(extensionRelease -> !extensionRelease.platforms.isEmpty())) {
                return Response.status(Response.Status.NOT_ACCEPTABLE)
                        .entity(entity)
                        .build();
            } else {
                ExtensionDeleteEvent event = new ExtensionDeleteEvent(extension);
                adminService.onExtensionDelete(event);
                return Response.accepted(entity).build();
            }
        }
    }

    @POST
    @Path("/v1/extension/compat")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @SecurityRequirement(name = "Authentication")
    @Operation(summary = "Flags an extension as compatible against a specific Quarkus version")
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
    @Operation(summary = "Flags an extension as incompatible against a specific Quarkus version")
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
    @Operation(summary = "Clear the maven cache")
    public Response clearCache() {
        cache.clear();
        return Response.accepted().build();
    }

    @PATCH
    @Path("/v1/stream/{platformKey}/{streamKey}")
    @SecurityRequirement(name = "Authentication")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    @Operation(summary = "Patches a PlatformStream")
    public Response patchPlatformStream(
            @NotNull(message = "platformKey is missing") @PathParam("platformKey") String platformKey,
            @NotNull(message = "streamKey is missing") @PathParam("streamKey") String streamKey,
            @FormParam("unlisted") boolean unlisted) {
        Platform platform = Platform.findByKey(platformKey)
                .orElseThrow(() -> new NotFoundException("Platform not found"));
        PlatformStream stream = PlatformStream.findByNaturalKey(platform, streamKey)
                .orElseThrow(() -> new NotFoundException("Platform Stream not found"));
        stream.unlisted = unlisted;
        try {
            stream.persist();
        } finally {
            DbState.updateUpdatedAt();
        }
        return Response.accepted().build();
    }
}
