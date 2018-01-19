package web.ejb.jar.bean;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;

/**
 * Bean implementation class for Enterprise Bean to be injected in SecurityEJBMExt07Bean for
 * ejb-jar.xml bindings and extension testing.
 */
@SuppressWarnings("deprecation")
@Stateless
public class SecurityEJBRunAsExtBean extends SecurityEJBBeanBase implements SecurityEJBRunAsInterface {

    private static final Class<?> c = SecurityEJBRunAsExtBean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @Resource
    private SessionContext context;

    public SecurityEJBRunAsExtBean() {
        withDeprecation();
    }

    @Override
    protected SessionContext getContext() {
        return context;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @RolesAllowed("Manager")
    public String manager() {
        return authenticate("Manager");
    }

    @RolesAllowed("Employee")
    public String employee() {
        return authenticate("Employee");
    }

    @RolesAllowed("DeclaredRole01")
    public String employeeAndManager() {
        return authenticate("EmployeeAndManager");
    }

}
