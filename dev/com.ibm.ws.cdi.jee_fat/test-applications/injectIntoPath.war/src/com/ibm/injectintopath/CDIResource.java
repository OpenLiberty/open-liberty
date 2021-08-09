package com.ibm.injectintopath;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;

@Path("cdiresource")
public class CDIResource {
	
    private ClassA classA;
	
    public CDIResource() {}
	
    @Inject
    public CDIResource(ClassA classA) {
        this.classA = classA;
    }

    @GET
    @Path("get")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
        System.out.println("GET!");
        System.out.println(classA.message());
        return Response.ok().type(MediaType.TEXT_PLAIN).entity(classA.message()).build();
    }
	
    @POST
    @Path("post")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post() {
        System.out.println("POST!");
        return Response.ok().build();
    }
}
