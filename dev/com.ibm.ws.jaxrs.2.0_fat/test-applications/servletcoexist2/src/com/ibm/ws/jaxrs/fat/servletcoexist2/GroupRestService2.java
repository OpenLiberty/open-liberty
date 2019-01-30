package com.ibm.ws.jaxrs.fat.servletcoexist2;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Path("/groups")
public class GroupRestService2 {

    @GET
    @Path("{id}")
    public Response getGroupById(@PathParam("id") String id) {

        return Response.status(200).entity("servletcoexist2 getGroupById is called, id is " + id).build();

    }

}