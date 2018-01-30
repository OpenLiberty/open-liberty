/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package web.ejb.jar.bean;

import java.security.Principal;
import java.util.logging.Logger;

import javax.security.enterprise.SecurityContext;

/**
 *
 */
public abstract class SecurityEJBBeanBase {

    private class Authentication {

        protected String authenticate(String method, SecurityContext context, Logger logger) {
            Principal principal = context.getCallerPrincipal();
            String principalName = null;
            if (principal != null) {
                principalName = principal.getName();
            } else {
                principalName = "null";
            }

            boolean isManager = false;
            boolean isEmployee = false;
            isManager = context.isCallerInRole("Manager");
            isEmployee = context.isCallerInRole("Employee");
            int len = principalName.length() + 12;
            StringBuffer result = new StringBuffer(len);
            result.append("EJB  = " + SecurityEJBBeanBase.this.getClass().getSimpleName() + "\n");
            result.append("Method = " + method + "\n");
            result.append("   getCallerPrincipal()=");
            result.append(principalName);
            result.append("\n");

            dealWithIdentity(result, context);

            result.append("   isCallerInRole(Manager)=");
            result.append(isManager);
            result.append("\n");
            result.append("   isCallerInRole(Employee)=");
            result.append(isEmployee);
            result.append("\n");
            logger.info("result: " + result);
            return result.toString();
        }

        protected void dealWithIdentity(StringBuffer result, SecurityContext context) {

        }
    }

    private final Authentication a;

    protected SecurityEJBBeanBase() {
        a = new Authentication();
    }

    protected abstract SecurityContext getContext();

    protected abstract Logger getLogger();

    protected String authenticate(String method) {
        return a.authenticate(method, getContext(), getLogger());
    }
}
