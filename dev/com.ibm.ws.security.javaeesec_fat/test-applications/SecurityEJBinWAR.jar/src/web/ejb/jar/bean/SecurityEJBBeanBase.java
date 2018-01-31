/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.ejb.jar.bean;

import java.security.Principal;
import java.util.logging.Logger;

import javax.ejb.SessionContext;

/**
 *
 */
public abstract class SecurityEJBBeanBase {

    private class Authentication {

        protected String authenticate(String method, SessionContext context, Logger logger) {
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
            result.append("   isCallerInRole(**)=");
            result.append(context.isCallerInRole("**"));
            result.append("\n");
            logger.info("result: " + result);
            return result.toString();
        }

        protected void dealWithIdentity(StringBuffer result, SessionContext context) {

        }
    }

    private class AuthenticationWithDeprecatedAPI extends Authentication {
        @SuppressWarnings("deprecation")
        @Override
        protected void dealWithIdentity(StringBuffer result, SessionContext context) {
            java.security.Identity identity = context.getCallerIdentity();
            String identityName = null;
            if (identity != null) {
                identityName = identity.getName();
            } else {
                identityName = "null";
            }
            result.append("   getCallerIdentity()=");
            result.append(identityName);
            result.append("\n");
        }
    }

    private Authentication a;

    protected SecurityEJBBeanBase() {
        a = new Authentication();
    }

    protected abstract SessionContext getContext();

    protected abstract Logger getLogger();

    public void withDeprecation() {
        a = new AuthenticationWithDeprecatedAPI();
    }

    protected String authenticate(String method) {
        return a.authenticate(method, getContext(), getLogger());
    }
}
