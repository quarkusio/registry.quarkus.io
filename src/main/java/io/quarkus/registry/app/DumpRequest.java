package io.quarkus.registry.app;

import org.eclipse.microprofile.openapi.annotations.Operation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/dump-request")
public class DumpRequest {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(hidden = true)
    public Response dump(@Context javax.ws.rs.core.HttpHeaders headers) {
        // Dump Request Headers
        StringBuilder sb = new StringBuilder();
        sb.append("Request Headers:\n");
        for (String name : headers.getRequestHeaders().keySet()) {
            sb.append(name).append(": ").append(headers.getRequestHeader(name)).append("\n");
        }
        return Response.ok(sb.toString()).build();
    }
}
