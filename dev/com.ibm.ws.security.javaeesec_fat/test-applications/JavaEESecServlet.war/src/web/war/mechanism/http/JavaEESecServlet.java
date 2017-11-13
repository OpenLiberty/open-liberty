package web.war.mechanism.http;

import javax.inject.Inject;

public class JavaEESecServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecServlet() {
        super("JavaEESecServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
