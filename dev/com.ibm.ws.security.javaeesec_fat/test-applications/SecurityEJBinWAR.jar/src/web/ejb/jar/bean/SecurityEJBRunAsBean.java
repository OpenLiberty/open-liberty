package web.ejb.jar.bean;

import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;

/**
 * Bean implementation class for Enterprise Bean
 */
@SuppressWarnings("deprecation")
@Stateless
public class SecurityEJBRunAsBean extends SecurityEJBBeanBase implements SecurityEJBRunAsInterface {

    private static final Class<?> c = SecurityEJBRunAsBean.class;
    protected Logger logger = Logger.getLogger(c.getCanonicalName());

    @Resource
    private SessionContext context;
    @Inject
    private SecurityContext securityContext;

    public SecurityEJBRunAsBean() {
        withDeprecation();
    }

    @Override
    protected SessionContext getContext() {
        return context;
    }

    @Override
    protected SecurityContext getSecurityContext() {
        return securityContext;
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    @Override
    @RolesAllowed("Manager")
    public String manager() {
        return authenticate("Manager");
    }

    @Override
    @RolesAllowed("Employee")
    public String employee() {
        return authenticate("Employee");
    }

    @Override
    @RolesAllowed({ "Employee", "Manager" })
    public String employeeAndManager() {
        return authenticate("Employee");
    }

}
