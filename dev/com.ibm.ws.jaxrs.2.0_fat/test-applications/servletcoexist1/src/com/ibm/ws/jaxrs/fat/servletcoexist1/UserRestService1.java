package com.ibm.ws.jaxrs.fat.servletcoexist1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/users")
public class UserRestService1 {

    @GET
    @Path("{id}")
    public Response getUserById(@PathParam("id") String id) {

        return Response.status(200).entity("servletcoexist1 getUserById is called, id is " + id).build();

    }

}