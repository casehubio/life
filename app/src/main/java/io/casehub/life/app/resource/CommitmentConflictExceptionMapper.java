package io.casehub.life.app.resource;

import io.casehub.life.app.commitment.CommitmentConflictException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CommitmentConflictExceptionMapper implements ExceptionMapper<CommitmentConflictException> {

    @Override
    public Response toResponse(CommitmentConflictException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorBody(exception.getMessage()))
                .build();
    }

    record ErrorBody(String message) {}
}
