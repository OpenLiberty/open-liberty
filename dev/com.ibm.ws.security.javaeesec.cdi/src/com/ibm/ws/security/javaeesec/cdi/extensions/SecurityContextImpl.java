/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.security.authorization.AuthorizationService;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.intfc.SubjectManagerService;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.metadata.MatchResponse;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.ws.webcontainer.security.util.WebConfigUtils;

/**
 *
 */
public class SecurityContextImpl implements SecurityContext {

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
        return SubjectManager.getCallerPrincipal(callerSubject);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#getPrincipalsByType(java.lang.Class)
     */
    @Override
    public <T extends Principal> Set<T> getPrincipalsByType(Class<T> type) {
        //Get the caller principal from the caller subject
        Subject callerSubject = getCallerSubject();

        if (callerSubject != null) {
            //Get the prinicipals by type
            Set<T> principals = callerSubject.getPrincipals(type);
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
    public boolean hasAccessToWebResource(String resource, String... methods) {

        String appName = getApplicationName();
        SecurityMetadata securityMetadata = WebConfigUtils.getSecurityMetadata();
        SecurityConstraintCollection collection = null;
        if (securityMetadata != null) {
            collection = securityMetadata.getSecurityConstraintCollection();
        }
        if (null != collection) {

            AuthorizationService authService = SecurityContextHelper.getAuthorizationService();
            Subject callerSubject = getCallerSubject();

            List<MatchResponse> matchResponses = collection.getMatchResponses(resource, methods);

            for (MatchResponse response : matchResponses) {

                if (response.equals(MatchResponse.NO_MATCH_RESPONSE)) {
                    // There are no constraints so user has access
                    return true;
                }

                if (response.isAccessPrecluded()) {
                    //This methods access is precluded proceed to next method
                    continue;
                }

                List<String> roles = response.getRoles();

                if (roles != null && !roles.isEmpty()) {
                    if (authService.isAuthorized(appName, roles, callerSubject)) {
                        return true;
                    }
                } else {
                    // there are no roles and access is not precluded the user has access
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.SecurityContext#isCallerInRole(java.lang.String)
     */
    @Override
    public boolean isCallerInRole(String role) {

        Subject callerSubject = getCallerSubject();
        AuthorizationService authService = SecurityContextHelper.getAuthorizationService();
        if (authService != null) {
            String appName = getApplicationName();
            List<String> roles = new ArrayList<String>();
            roles.add(role);

            return authService.isAuthorized(appName, roles, callerSubject);
        }
        return false;
    }

    private Subject getCallerSubject() {
        SubjectManagerService subjectManagerService = SecurityContextHelper.getSubjectManagerService();
        if (subjectManagerService != null) {
            Subject callerSubject = null;

            callerSubject = subjectManagerService.getCallerSubject();
            return callerSubject;
        }
        return null;
    }

    private String getApplicationName() {
        ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        return cmd.getJ2EEName().getApplication();
    }
}
