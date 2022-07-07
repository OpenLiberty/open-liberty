package restClient;

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

@Path("client")
public class ClientEndpoints {

	@Inject
	@RestClient
	public RESTclient restClient;
	
	@Inject
	@ConfigProperty(name = "default.http.port")
	public String port;
	
	@GET
	@Path("default")
	@Produces(MediaType.APPLICATION_JSON)
	public String produceOutput() {
	    try {
	        return restClient.getProperties();
	    } catch (Exception e) {
	        e.printStackTrace();
	        return "Exception Thrown";
	    }
	}
	
	@GET
	@Path("{host}")
	@Produces(MediaType.APPLICATION_JSON)
	public String queryHost(@PathParam(value="host") String baseURI) {
	    String customURIString = "http://localhost:" + port + "/webappWAR/" + baseURI;
	    try {
	      URI customURI = URI.create(customURIString);
	      RESTclient customRestClient = RestClientBuilder.newBuilder()
                  .baseUri(customURI)
                  .build(RESTclient.class);
	      return customRestClient.getProperties();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    	return "Exception Thrown";
	    }
	}
	
}