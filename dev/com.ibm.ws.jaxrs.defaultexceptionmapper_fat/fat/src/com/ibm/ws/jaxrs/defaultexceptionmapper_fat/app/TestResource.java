package com.ibm.ws.jaxrs.defaultexceptionmapper_fat.app;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class TestResource {

    @GET
    @Path("causeException")
    @Produces(MediaType.TEXT_PLAIN)
    public String causeException() {
        // This exception should be caught by the default exception handler
        throw new RuntimeException("Test Exception");
    }

    @GET
    @Path("handledException")
    @Produces(MediaType.TEXT_PLAIN)
    public String handledException() {
        // This exception should be handled by the HandledExceptionMapper,
        // so it should not be seen by the default exception handler
        throw new HandledException("Handled Exception");
    }

}
