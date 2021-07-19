package web;

public class AuditFormLoginServlet extends FlexibleBaseServlet {
    private static final long serialVersionUID = 1L;

    public AuditFormLoginServlet() {
        super("AuditFormLoginServlet");

        mySteps.add(new WriteRequestBasicsStep());
        mySteps.add(new WritePrincipalStep());
        mySteps.add(new WriteRolesStep());

    }
}
