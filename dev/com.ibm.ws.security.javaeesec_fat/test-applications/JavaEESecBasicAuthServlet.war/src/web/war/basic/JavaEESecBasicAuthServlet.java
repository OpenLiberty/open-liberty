package web.war.basic;

import web.jar.base.FlexibleBaseServlet;
import javax.inject.Inject;

public class JavaEESecBasicAuthServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecBasicAuthServlet() {
        super("JavaEESecBasicAuthServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSecurityContextStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
