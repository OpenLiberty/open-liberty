package web.war.annotatedbasic.dbauth;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@DatabaseIdentityStoreDefinition(
                                 callerQuery = "select password from callertable where name = ?",
                                 groupsQuery = "select group_name from callertable_groups where caller_name = ?",
                                 dataSourceLookup = "jdbc/derby1fat")

public class DatabaseAuthAliasBasicAuthServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public DatabaseAuthAliasBasicAuthServlet() {

        super("DatabaseAuthAliasBasicAuthServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }

}
