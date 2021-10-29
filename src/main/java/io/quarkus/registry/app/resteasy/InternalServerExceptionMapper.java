package io.quarkus.registry.app.resteasy;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import io.quarkus.logging.Log;

@Provider
public class InternalServerExceptionMapper implements ExceptionMapper<InternalServerErrorException> {
    @Override
    public Response toResponse(InternalServerErrorException exception) {
        Log.error("Internal server error occurred", exception.getCause());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
