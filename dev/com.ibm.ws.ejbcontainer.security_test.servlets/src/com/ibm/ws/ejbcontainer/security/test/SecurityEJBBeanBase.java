/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.ejbcontainer.security.test;

import java.security.Principal;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.SessionContext;
import javax.security.jacc.PolicyContext;

/**
 *
 */
public abstract class SecurityEJBBeanBase {

    /**
     * Are we running with <code>jakarta.ejb.*</code> packages? This will indicate we are running with (at least) Jakarta EE 9.
     *
     * This check may seem silly on the surface, but the packages are transformed at run time to swap the <code>javax.ejb.*</code> packages with
     * <code>jakarta.ejb.*</code>.
     */
    private static boolean isEENineOrHigher = SessionContext.class.getCanonicalName().startsWith("jakarta.ejb");

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
            Set<String> handlerKeys = PolicyContext.getHandlerKeys();
            for (String key : handlerKeys) {
                result.append("handlerKey(");
                result.append(key);
                result.append(")=true\n");
            }
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
        /*
         * Jakarta EE 9 removed the (deprecated) SessionContext.getCallerIdentity()
         * method, so don't ever call it when running Jakarta EE 9+.
         */
        if (!isEENineOrHigher) {
            a = new AuthenticationWithDeprecatedAPI();
        }
    }

    protected String authenticate(String method) {
        return a.authenticate(method, getContext(), getLogger());
    }
}
