package declared.roles;

import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.RunAs;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;

@Path("/runas")
public class RunAsResource {

    @Inject
    private RunAsBean bean;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Set<String> getCallerRoles() {
        return bean.getRoles();
    }
}
