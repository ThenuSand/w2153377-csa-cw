package com.westminster.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

/**
 * Logs every inbound request and the status of its corresponding
 * outbound response. Because this is a JAX-RS Provider, every resource
 * method benefits from it without any per-method plumbing.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOGGER.info("REQUEST  " + requestContext.getMethod()
                + "  " + requestContext.getUriInfo().getRequestUri());
    }

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        LOGGER.info("RESPONSE " + requestContext.getMethod()
                + "  " + requestContext.getUriInfo().getRequestUri()
                + "  -> " + responseContext.getStatus());
    }
}
