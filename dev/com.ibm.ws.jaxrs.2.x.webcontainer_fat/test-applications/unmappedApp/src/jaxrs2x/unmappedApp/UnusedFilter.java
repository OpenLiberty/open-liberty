/**
 *
 */
package jaxrs2x.unmappedApp;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

/**
 * Notice no {@code @Provider} annotation - so this would only be registered if the {@link UnmappedApplication} were used
 */
public class UnusedFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext crc) throws IOException {
        crc.abortWith(Response.serverError().entity("UnusedFilter invoked but shouldn't have been...").build());
    }
}
