package restClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
    
@RegisterRestClient(baseUri = "http://localhost:8010/webappWAR/endpoint")
@Path("/server")
public interface RESTclient extends AutoCloseable {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/properties")
    public String getProperties() throws Exception;

}
