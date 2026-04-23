package com.westminster.smartcampus.mapper;

import com.westminster.smartcampus.model.ErrorResponse;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Turns the built-in javax.ws.rs.NotFoundException (thrown by our
 * resources when a path parameter points at nothing) into a nice JSON
 * 404 instead of Jersey's default HTML page.
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Resource not found.";

        ErrorResponse body = new ErrorResponse(
                Response.Status.NOT_FOUND.getStatusCode(),
                "RESOURCE_NOT_FOUND",
                message
        );

        return Response.status(Response.Status.NOT_FOUND)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
