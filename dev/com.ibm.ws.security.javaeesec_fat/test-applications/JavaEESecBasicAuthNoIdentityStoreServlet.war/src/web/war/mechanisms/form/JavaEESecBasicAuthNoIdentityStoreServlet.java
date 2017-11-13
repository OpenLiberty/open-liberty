package web.war.mechanisms.form;

import javax.inject.Inject;

public class JavaEESecBasicAuthNoIdentityStoreServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public JavaEESecBasicAuthNoIdentityStoreServlet() {
        super("JavaEESecBasicAuthNoIdentityStoreServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
        mySteps.add(new WriteSubjectStep());
        mySteps.add(new WritePublicCredentialsStep());
        mySteps.add(new WriteRunAsSubjectStep());
        mySteps.add(new WriteCookiesStep());
    }
}
