package io.casehub.life.app.resource;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Workaround for casehub-ledger-rest's LedgerExceptionMapper which catches all
 * RuntimeException and converts unrecognized types to 500. This more-specific
 * mapper wins JAX-RS resolution for WebApplicationException and its subclasses
 * (NotFoundException, ClientErrorException, etc.), preserving the intended status code.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        return exception.getResponse();
    }
}
