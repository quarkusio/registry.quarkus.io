package io.quarkus.registry.app.resteasy;

import io.quarkus.logging.Log;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class InternalServerExceptionMapper implements ExceptionMapper<InternalServerErrorException> {
    @Override
    public Response toResponse(InternalServerErrorException exception) {
        Log.error("Internal server error occurred", exception.getCause());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
