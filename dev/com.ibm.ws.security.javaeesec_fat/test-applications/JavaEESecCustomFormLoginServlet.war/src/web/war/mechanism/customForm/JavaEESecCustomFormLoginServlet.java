package web.war.mechanism.customForm;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@CustomFormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/customLoginError.jsp", loginPage="/customLogin.xhtml", useForwardToLogin=false, useForwardToLoginExpression="#{elForward}"))
@LdapIdentityStoreDefinition(bindDn="cn=root", bindDnPassword="security")
public class JavaEESecCustomFormLoginServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecCustomFormLoginServlet() {
        super("JavaEESecAnnotatedFormLoginServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }
}
