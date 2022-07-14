package restClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import restClient.RESTclient;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.POST;

@Path("client")
@ApplicationScoped
public class ClientEndpoints {

	@Inject
	@RestClient
	private RESTclient restClient;
	
	@Inject
	@ConfigProperty(name = "default.http.port")
	private String port;
	
	@GET
	@Path("properties")
	@Produces(MediaType.APPLICATION_JSON)
	public String produceOutput() {
	    try {
	        return restClient.getProperties();
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "Exception Thrown";
	    }
	}
	
	@POST
	@Path("setHost/{host}")
	public void setHost(@PathParam(value="host") String baseURI) {
	    String customURIString = "http://localhost:" + port + "/webappWAR/" + baseURI;
	    URI customURI = URI.create(customURIString);
	    RESTclient customRestClient = RestClientBuilder.newBuilder()
                  .baseUri(customURI)
                  .build(RESTclient.class);
	    restClient = customRestClient;
	}
	
}