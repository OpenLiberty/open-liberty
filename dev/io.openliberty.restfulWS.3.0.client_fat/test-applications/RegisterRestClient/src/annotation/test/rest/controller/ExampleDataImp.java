package annotation.test.rest.controller;

import annotation.test.rest.model.ExampleData;
import annotation.test.rest.stub.ExampleDataStub;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.ArrayList;
import java.util.List;

@Path("lookup")
public class ExampleDataImp {

    @Inject
    @RestClient
    private ExampleDataStub proxy;

    private List<ExampleData> series = new ArrayList();
    private  ExampleData entry;
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get() {
            ExampleData entry = proxy.get("some data");
            return Response.ok(entry).build();    
    }
}
