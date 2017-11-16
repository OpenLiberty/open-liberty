package web.war.servlets.form.forward;
import web.jar.base.FlexibleBaseServlet;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=true, useForwardToLoginExpression=""))
@WebServlet("/MultipleISFormServlet")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = {@HttpMethodConstraint(value = "GET", rolesAllowed = "grantedgroup")})
public class MultipleISFormServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public MultipleISFormServlet() {
        super("MultipleISFormServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
        mySteps.add(new WriteJSR375Step());
    }
}
