package web.war.mechanism.customForm;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@CustomFormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/customLoginError.jsp", loginPage="/customLogin.xhtml", useForwardToLogin=true, useForwardToLoginExpression="#{elForward}"))
@LdapIdentityStoreDefinition(bindDn="cn=root", bindDnPassword="security")
public class JavaEESecCustomFormLoginForwardServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecCustomFormLoginForwardServlet() {
        super("JavaEESecCustomFormLoginForwardServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }
}
