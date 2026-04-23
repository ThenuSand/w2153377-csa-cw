package com.westminster.smartcampus.mapper;

import com.westminster.smartcampus.model.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global safety net. Catches anything no more specific mapper handles
 * so a runtime failure never leaks a Java stack trace to the client.
 *
 * NB: WebApplicationException-derived errors (Response objects that
 * the framework already shaped) pass straight through so we do not
 * accidentally rewrite a 404/405/etc. as a 500.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            // Let JAX-RS handle it with the status the exception already carries.
            return ((WebApplicationException) ex).getResponse();
        }

        LOGGER.log(Level.SEVERE, "Unhandled server error", ex);

        ErrorResponse body = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please contact the API administrator."
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
