package web.war.annotatedbasic;

import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import web.jar.base.FlexibleBaseServlet;

@BasicAuthenticationMechanismDefinition(realmName = "JavaEESec Basic Realm")
@LdapIdentityStoreDefinition(
                             url = "ldap://localhost:10389/",
                             callerBaseDn = "",
                             callerSearchBase = "o=ibm,c=us",
                             callerSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             callerSearchFilter = "(&(objectclass=person)(uid=%s))",
                             callerNameAttribute = "uid",
                             groupNameAttribute = "cn",
                             groupSearchBase = "o=ibm,c=us",
                             groupSearchScope = LdapIdentityStoreDefinition.LdapSearchScope.SUBTREE,
                             groupSearchFilter = "(objectclass=groupofnames)",
                             groupMemberAttribute = "member",
                             bindDn = "uid=jaspildapuser1,o=ibm,c=us",
                             bindDnPassword = "s3cur1ty")
public class JavaEESecAnnotatedBasicAuthServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecAnnotatedBasicAuthServlet() {
        super("JavaEESecAnnotatedBasicAuthServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
