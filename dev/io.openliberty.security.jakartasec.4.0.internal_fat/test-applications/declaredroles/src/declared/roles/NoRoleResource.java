package declared.roles;

import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Set;

@Path("/noroles")
public class NoRoleResource {

    @Inject
    private SecurityContext sc;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Set<String> authenticate(){
        return sc.getAllDeclaredCallerRoles();
    }

}
