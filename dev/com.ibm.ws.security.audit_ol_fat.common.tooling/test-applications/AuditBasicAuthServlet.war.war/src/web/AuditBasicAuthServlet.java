package web;

public class AuditBasicAuthServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public AuditBasicAuthServlet() {
        super("AuditBasicAuthServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());
    }
}
