package declared.roles;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;

@Path("/roles")
@RolesAllowed({"Role1", "Role2"})
public class RolesResource {

    @Inject
    SecurityContext sc;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Set<String> getCallerRoles() {
        return sc.getAllDeclaredCallerRoles();
    }
}
