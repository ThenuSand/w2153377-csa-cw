package com.westminster.smartcampus.mapper;

import com.westminster.smartcampus.exception.LinkedResourceNotFoundException;
import com.westminster.smartcampus.model.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps LinkedResourceNotFoundException -> 422 Unprocessable Entity.
 * 422 is more accurate than 404 when the payload itself was valid JSON
 * but refers to a resource that does not exist.
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    /** Jakarta's Response.Status enum does not include 422 so we inline the int. */
    public static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedResourceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(
                UNPROCESSABLE_ENTITY,
                "LINKED_RESOURCE_NOT_FOUND",
                ex.getMessage()
        );

        return Response.status(UNPROCESSABLE_ENTITY)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
