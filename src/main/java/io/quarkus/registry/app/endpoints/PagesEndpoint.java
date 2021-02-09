package io.quarkus.registry.app.endpoints;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.panache.common.Sort;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.registry.app.model.Extension;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/")
public class PagesEndpoint {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance index() {
        List<Extension> list = Extension.findAll(Sort.ascending("name")).list();
        return Templates.index(list);
    }

    @GET
    @Path("extension/{groupId}/{artifactId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance extensions(@PathParam String groupId, @PathParam String artifactId) {
        Extension extension = Extension.findByGA(groupId, artifactId)
                .orElseThrow(() -> new WebApplicationException(Response.Status.NOT_FOUND));
        return Templates.extensionDetail(extension);
    }
}
