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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 *
 */
public class SecurityContextImpl implements SecurityContext {
    private static final TraceComponent tc = Tr.register(SecurityContextImpl.class);

    private final SubjectManager subjectManager = null;

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

        Subject callerSubject = getCallerSubject();

        if (callerSubject == null) {
            return null;
        }

        Set<Principal> principals = callerSubject.getPrincipals();
        if (principals.size() > 0) {
            for (Iterator iterator = principals.iterator(); iterator.hasNext();) {
                Principal principal = (Principal) iterator.next();
                if (principal instanceof WSPrincipal)
                    return principal;
            }
            // There is no WSPrincipal so just return first one
            return principals.iterator().next();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#getPrincipalsByType(java.lang.Class)
     */
    @Override
    public <T extends Principal> Set<T> getPrincipalsByType(Class<T> arg0) {
        //Get the caller principal from the caller subject
        Subject callerSubject = getCallerSubject();

        if (callerSubject == null) {
            return null;
        }

        //Get the prinicipals by type
        if (callerSubject != null) {
            Set<T> principals = callerSubject.getPrincipals(arg0);
            return principals;
        }

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
    public boolean isCallerInRole(String role) {

        AuthorizationService authService = SecurityContextHelper.getAuthorizationService();
        if (authService != null) {
            Subject callerSubject = getCallerSubject();
            String appName = getApplicationName();
            List<String> roles = new ArrayList<String>();
            roles.add(role);

            if (callerSubject == null) {
                return false;
            }

            return authService.isAuthorized(appName, roles, callerSubject);
        }
        return false;
    }

    protected Subject getCallerSubject() {
        Subject callerSubject = null;
        try {
            callerSubject = WSSubject.getRunAsSubject();
            if (callerSubject == null) {
                callerSubject = WSSubject.getCallerSubject();
            }
        } catch (WSSecurityException wse) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception getting Subject", wse);
            }
        }
        return callerSubject;
    }

    protected String getApplicationName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return cmd.getJ2EEName().getApplication();
    }

}
