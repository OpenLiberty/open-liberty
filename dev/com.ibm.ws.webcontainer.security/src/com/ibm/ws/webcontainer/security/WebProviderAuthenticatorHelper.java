/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.Hashtable;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * The responsibility of this WebProviderAuthenticatorProxy is to authenticate request with TAI and SSO
 */
public class WebProviderAuthenticatorHelper {
    private static final TraceComponent tc = Tr.register(WebProviderAuthenticatorHelper.class);

    private final AtomicServiceReference<SecurityService> securityServiceRef;

    public WebProviderAuthenticatorHelper(AtomicServiceReference<SecurityService> securityServiceRef) {
        this.securityServiceRef = securityServiceRef;
    }

    /**
     * @param req
     * @param res
     * @param subject
     * @param userName
     * @param customCacheKey
     * @param mapIdentityToRegistryUser
     * @return
     */
    public AuthenticationResult loginWithUserName(HttpServletRequest req, HttpServletResponse res, String userName, Subject subj,
                                                  Hashtable<String, Object> customProperties,
                                                  boolean mapIdentityToRegistryUser) {
        Subject tempSubject = subj;
        if (tempSubject == null)
            tempSubject = new Subject();
        if (customProperties == null)
            customProperties = new Hashtable<String, Object>();

        SecurityService securityService = securityServiceRef.getService();
        AuthenticationService authenticationService = securityService.getAuthenticationService();

        updateHashtable(userName, customProperties, mapIdentityToRegistryUser, authenticationService);

        tempSubject.getPrivateCredentials().add(customProperties);

        Subject subject = authenticateWithSubject(req, res, tempSubject);
        if (subject == null) {
            return new AuthenticationResult(AuthResult.FAILURE, "subject is null");
        }

        removeSecurityNameAndUniquedIdFromHashtable(subject, customProperties, mapIdentityToRegistryUser);
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subject);
        return authResult;
    }

    public AuthenticationResult loginWithHashtable(HttpServletRequest req, HttpServletResponse res,
                                                   Subject partialSubject) {

        Subject subject = authenticateWithSubject(req, res, partialSubject);
        if (subject == null) {
            return new AuthenticationResult(AuthResult.FAILURE, "subject is null");
        }
        AuthenticationResult authResult = new AuthenticationResult(AuthResult.SUCCESS, subject);
        return authResult;
    }

    /**
     * @param userName
     * @param customCacheKey
     * @param mapIdentityToRegistryUser
     * @param authenticationService
     */
    private void updateHashtable(String userName, Hashtable<String, Object> hashtable, boolean mapIdentityToRegistryUser,
                                 AuthenticationService authenticationService) {
        if (mapIdentityToRegistryUser) {
            addUserOnlyToHashTable(userName, hashtable, authenticationService);
        } else {
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, userName);
            if (hashtable.get(AttributeNameConstants.WSCREDENTIAL_UNIQUEID) == null) {
                addUniqueIdToHashtable(hashtable, userName);
            }
        }
    }

    /**
     * @param hashtable
     * @param mapUserName
     */
    private void addUniqueIdToHashtable(Hashtable<String, Object> hashtable, String mapUserName) {

        String realm = "defaultRealm";
        try {
            realm = securityServiceRef.getService().getUserRegistryService().getUserRegistry().getRealm();
        } catch (RegistryException e) {
            //do nothing
        }
        String uniqueID = new StringBuffer("user:").append(realm).append("/").append(mapUserName).toString();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
    }

    /**
     * @param userName
     * @param hashtable
     * @param authenticationService
     */
    private void addUserOnlyToHashTable(String userName, Hashtable<String, Object> hashtable, AuthenticationService authenticationService) {
        if (!authenticationService.isAllowHashTableLoginWithIdOnly()) {
            hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        }
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, userName);
    }

    private void removeSecurityNameAndUniquedIdFromHashtable(Subject subject, Hashtable<String, ?> props, boolean mapIdentityToRegistryUser) {
        if (!mapIdentityToRegistryUser && !subject.isReadOnly()) {
            Set<Object> privateCredentials = subject.getPrivateCredentials();
            privateCredentials.remove(props);
            props.remove(AttributeNameConstants.WSCREDENTIAL_UNIQUEID);
            props.remove(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME);
            if (!props.isEmpty()) {
                privateCredentials.add(props);
            }
        }
    }

    @FFDCIgnore(AuthenticationException.class)
    private Subject authenticateWithSubject(HttpServletRequest req, HttpServletResponse res, Subject subject) {
        Subject new_subject = null;
        try {
            AuthenticationData authenticationData = createAuthenticationData(req, res);
            new_subject = securityServiceRef.getService().getAuthenticationService().authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, authenticationData, subject);
        } catch (AuthenticationException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception when performing authenticateWithSubject.", e);
            }
        }
        return new_subject;
    }

    @Trivial
    protected AuthenticationData createAuthenticationData(HttpServletRequest req, HttpServletResponse res) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, req);
        authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, res);
        return authenticationData;
    }
}
