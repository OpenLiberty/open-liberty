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
package com.ibm.ws.security.javaeesec.cdi.beans;

import java.security.Principal;
import java.util.Set;

import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;

/**
 *
 */
public class SecurityContextImpl implements SecurityContext {
    private static final TraceComponent tc = Tr.register(SecurityContextImpl.class);

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#authenticate(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse,
     * javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters)
     */
    @Override
    public AuthenticationStatus authenticate(HttpServletRequest req, HttpServletResponse res, AuthenticationParameters params) {
        AuthenticationStatus authStatus = AuthenticationStatus.SEND_FAILURE;
        req.setAttribute(JavaEESecConstants.SECURITY_CONTEXT_AUTH_PARAMS, params);
        try {
            boolean result = req.authenticate(res);
            if (result) {
                authStatus = AuthenticationStatus.SUCCESS;
            } else {
//TODO some error handling.
            }
        } catch (Exception e) {
//TODO need to handle error.
            e.printStackTrace();
        }
        return authStatus;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#getCallerPrincipal()
     */
    @Override
    public Principal getCallerPrincipal() {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#getPrincipalsByType(java.lang.Class)
     */
    @Override
    public <T extends Principal> Set<T> getPrincipalsByType(Class<T> arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#hasAccessToWebResource(java.lang.String, java.lang.String[])
     */
    @Override
    public boolean hasAccessToWebResource(String arg0, String... arg1) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#isCallerInRole(java.lang.String)
     */
    @Override
    public boolean isCallerInRole(String arg0) {
        // TODO Auto-generated method stub
        return false;
    }

}
