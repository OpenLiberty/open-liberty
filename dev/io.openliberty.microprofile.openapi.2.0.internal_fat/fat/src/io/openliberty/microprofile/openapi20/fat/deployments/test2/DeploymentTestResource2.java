package io.openliberty.microprofile.openapi20.fat.deployments.test2;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/")
public class DeploymentTestResource2 {

    @GET
    @Path("/test2")
    @Operation(summary = "test method")
    @APIResponse(responseCode = "200", description = "constant \"OK\" response")
    @Produces(value = MediaType.TEXT_PLAIN)
    public String test2() {
        return "OK";
    }

}
