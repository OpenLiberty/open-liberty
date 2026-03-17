package declared.roles;

import jakarta.annotation.security.RunAs;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.SecurityContext;

import java.util.Set;

@RequestScoped
@RunAs("user4")
public class RunAsBean {

    @Inject
    SecurityContext sc;

    public Set<String> getRoles(){
        return sc.getAllDeclaredCallerRoles();
    }
}
