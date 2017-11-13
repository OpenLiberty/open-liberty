package web.war.mechanisms.form;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=true, useForwardToLoginExpression="#{elForward}"))
public class JavaEESecAnnotatedFormLoginForwardServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecAnnotatedFormLoginForwardServlet() {
        super("JavaEESecAnnotatedFormLoginForwardServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WriteRunAsSubjectStep());
    }
}
