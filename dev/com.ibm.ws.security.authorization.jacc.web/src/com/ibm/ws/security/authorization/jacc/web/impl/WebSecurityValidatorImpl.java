/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
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
package com.ibm.ws.security.authorization.jacc.web.impl;

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.WebUserDataPermission;
import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authorization.jacc.common.PolicyContextHandlerImpl;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityValidator;

public class WebSecurityValidatorImpl implements WebSecurityValidator {
    private static final TraceComponent tc = Tr.register(WebSecurityValidatorImpl.class);
    private static String[] jaccHandlerKeyArrayEe8 = new String[] { "javax.security.auth.Subject.container", "javax.servlet.http.HttpServletRequest" };
    private static String[] jaccHandlerKeyArrayEe9 = new String[] { "javax.security.auth.Subject.container", "jakarta.servlet.http.HttpServletRequest" };
    private static PolicyContextHandlerImpl pch = PolicyContextHandlerImpl.getInstance();

    public WebSecurityValidatorImpl() {
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkDataConstraints(String contextId, Object httpServletRequest, WebUserDataPermission webUDPermission, PolicyProxy policyProxy) {
        HttpServletRequest req = null;
        if (httpServletRequest != null) {
            try {
                req = (HttpServletRequest) httpServletRequest;
            } catch (ClassCastException cce) {
                Tr.error(tc, "JACC_WEB_SPI_PARAMETER_ERROR", new Object[] { httpServletRequest.getClass().getName(), "checkDataConstraints", "HttpServletRequest" });
                return false;
            }
        }

        // In this method, we check for the following constraints
        // 1. Data Constraints (is SSL required?)
        Boolean result = Boolean.FALSE;
        try {
            final WebUserDataPermission wudp = webUDPermission;
            final String fci = contextId;
            final HashMap<String, HttpServletRequest> handlerObjects = new HashMap<String, HttpServletRequest>();
            final HttpServletRequest hsr = req;
            result = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws javax.security.jacc.PolicyContextException {
                    PolicyContext.setContextID(fci);
                    for (String jaccHandlerKey : jaccHandlerKeyArrayEe8) {
                        PolicyContext.registerHandler(jaccHandlerKey, pch, true);
                    }
                    for (String jaccHandlerKey : jaccHandlerKeyArrayEe9) {
                        PolicyContext.registerHandler(jaccHandlerKey, pch, true);
                    }
                    handlerObjects.put(jaccHandlerKeyArrayEe8[1], hsr);
                    handlerObjects.put(jaccHandlerKeyArrayEe9[1], hsr);
                    PolicyContext.setHandlerData(handlerObjects);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Calling JACC implies");
                    return Boolean.valueOf(policyProxy.implies(contextId, null, wudp));
                }
            });

        } catch (PrivilegedActionException e) {
            Tr.error(tc, "JACC_WEB_IMPLIES_FAILURE", new Object[] { contextId, e.getException() });
            result = Boolean.FALSE;
        }

        return result.booleanValue();
    }

    /** {@inheritDoc} */
    @Override
    public boolean checkResourceConstraints(String contextId, Object httpServletRequest, Permission webPerm, Subject subject, PolicyProxy policyProxy) {
        HttpServletRequest req = null;
        if (httpServletRequest != null) {
            try {
                req = (HttpServletRequest) httpServletRequest;
            } catch (ClassCastException cce) {
                Tr.error(tc, "JACC_WEB_SPI_PARAMETER_ERROR", new Object[] { httpServletRequest.getClass().getName(), "checkDataConstraints", "HttpServletRequest" });
                return false;
            }
        }
        boolean result = false;
        try {
            final HashMap<String, Object> ho = new HashMap<String, Object>();
            final Subject s = subject;
            final String cid = contextId;
            final Permission p = webPerm;
            final HttpServletRequest r = req;
            result = checkResourceConstraints(cid, r, p, s, ho, policyProxy);
        } catch (PrivilegedActionException e) {
            Tr.error(tc, "JACC_WEB_IMPLIES_FAILURE", new Object[] { contextId, e.getException() });
        }
        return result;
    }

    private boolean checkResourceConstraints(final String contextId,
                                             final HttpServletRequest req,
                                             final Permission permission,
                                             final Subject subject,
                                             final HashMap<String, Object> handlerObjects,
                                             PolicyProxy policyProxy) throws PrivilegedActionException {
        Boolean result = Boolean.FALSE;
        result = AccessController.doPrivileged(
                                               new PrivilegedExceptionAction<Boolean>() {
                                                   @Override
                                                   public Boolean run() throws javax.security.jacc.PolicyContextException {
                                                       PolicyContext.setContextID(contextId);

                                                       if (tc.isDebugEnabled())
                                                           Tr.debug(tc, "Registering JACC context handlers");
                                                       for (String key : jaccHandlerKeyArrayEe8) {
                                                           PolicyContext.registerHandler(key, pch, true);
                                                       }
                                                       for (String key : jaccHandlerKeyArrayEe9) {
                                                           PolicyContext.registerHandler(key, pch, true);
                                                       }

                                                       handlerObjects.put(jaccHandlerKeyArrayEe8[0], subject);
                                                       handlerObjects.put(jaccHandlerKeyArrayEe8[1], req);

                                                       handlerObjects.put(jaccHandlerKeyArrayEe9[0], subject);
                                                       handlerObjects.put(jaccHandlerKeyArrayEe9[1], req);

                                                       if (tc.isDebugEnabled())
                                                           Tr.debug(tc, "Setting JACC handler data");
                                                       PolicyContext.setHandlerData(handlerObjects);
                                                       if (tc.isDebugEnabled())
                                                           Tr.debug(tc, "Calling JACC implies. Subject : " + subject);
                                                       return policyProxy.implies(contextId, subject, permission);
                                                   }
                                               });
        return result.booleanValue();
    }
}
