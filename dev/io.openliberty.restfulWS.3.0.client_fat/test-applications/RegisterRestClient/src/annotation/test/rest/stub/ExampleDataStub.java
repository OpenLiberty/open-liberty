package annotation.test.rest.stub;

import annotation.test.rest.model.ExampleData;
import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(baseUri = "https://api.ibmtest.com/singlesearch/")
public interface ExampleDataStub extends AutoCloseable{

    @Path("shows")
    @GET
    ExampleData get(@QueryParam("q") String title);
}
