/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * @version 1.0.0
 */
package com.ibm.ws.security.common.token.propagation;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.common.TraceConstants;

@Component(configurationPid = "com.ibm.ws.security.common.token.propagation.tokenpropagationhelper", configurationPolicy = ConfigurationPolicy.OPTIONAL, name = "TokenPropagationHelper", service = TokenPropagationHelper.class, immediate = true, property = { "service.vendor=IBM" })
public class TokenPropagationHelper {
    private static volatile SecurityService securityService;
    public static final TraceComponent tc = Tr.register(TokenPropagationHelper.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);
    private static final String KEY_SECURITY_SERVICE = "securityService";

    public static final String JWT_TOKEN = "jwt"; // jwt token issued by the provider
    public static final String ISSUED_JWT_TOKEN = "issuedJwt"; // new jwt token

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> properties) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> properties) {
        // do nothing for now
    }

    // serviceReferences are bad, avoid and do this instead. 
    @Reference(name = KEY_SECURITY_SERVICE, policy = ReferencePolicy.DYNAMIC)
    protected void setSecurityService(SecurityService securitysvc) {
        securityService = securitysvc;
    }

    protected void unsetSecurityService(SecurityService securitysvc) {
        securityService = null;
    }

    /**
     * Get the type of access token which the runAsSubject authenticated
     * 
     * @return the Type of Token, such as: Bearer
     * @throws Exception
     */
    public static String getAccessTokenType() throws Exception {
        return getSubjectAttributeString("token_type", true);
    }

    public static String getAccessToken() throws Exception {
        return getSubjectAttributeString("access_token", true);
    }

    public static String getJwtToken() throws Exception {
        String jwt = getIssuedJwtToken();
        if (jwt == null) {
            jwt = getAccessToken(); // the one that the client received
            if (!isJwt(jwt)) {
                jwt = null;
            }
        }
        return jwt;
    }

    private static boolean isJwt(String jwt) {
        if (jwt != null && jwt.indexOf(".") >= 0) {
            return true;
        }
        return false;
    }

    public static String getIssuedJwtToken() throws Exception {
        return getSubjectAttributeString(ISSUED_JWT_TOKEN, true); // the newly issued token
    }

    public static String getScopes() throws Exception {
        return getSubjectAttributeString("scope", true);
    }

    public static Subject getRunAsSubject() {
        try {
            return getRunAsSubjectInternal();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Received Exception retrieving subject: " + e);
            }
            return null;
        }
    }

    static Subject getRunAsSubjectInternal() throws Exception {
        Subject sub = null;
        try {
            sub = (Subject) AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            return WSSubject.getRunAsSubject();

                        }
                    });
        } catch (PrivilegedActionException e) {
            throw new Exception(e.getCause());
        }
        return sub;

    }

    /**
     * Gets the username from the principal of the subject.
     * 
     * @return
     * @throws WSSecurityException
     */
    public static String getUserName() throws Exception {
        Subject subject = getRunAsSubjectInternal();
        if (subject == null) {
            return null;
        }
        Set<Principal> principals = subject.getPrincipals();
        Iterator<Principal> principalsIterator = principals.iterator();
        if (principalsIterator.hasNext()) {
            Principal principal = principalsIterator.next();
            return principal.getName();
        }
        return null;
    }

    /**
     * @param string
     * @return
     * @throws Throwable
     */
    static String getSubjectAttributeString(String attribKey, boolean bindWithAccessToken) throws Exception {
        Subject runAsSubject = getRunAsSubjectInternal();

        if (runAsSubject != null) {
            return getSubjectAttributeObject(runAsSubject, attribKey, bindWithAccessToken);
        }
        return null;
    }

    /**
     * @param runAsSubject
     * @param attribKey
     * @return object
     * @throws Throwable
     */
    @FFDCIgnore({ PrivilegedActionException.class })
    static String getSubjectAttributeObject(Subject subject, String attribKey, boolean bindWithAccessToken) throws Exception {
        try {
            Set<Object> publicCredentials = subject.getPublicCredentials();
            String result = getCredentialAttribute(publicCredentials, attribKey, bindWithAccessToken, "publicCredentials");
            if (result == null || result.isEmpty()) {
                Set<Object> privateCredentials = subject.getPrivateCredentials();
                result = getCredentialAttribute(privateCredentials, attribKey, bindWithAccessToken, "privateCredentials");
            }
            return result;
        } catch (PrivilegedActionException e) {
            // TODO do we need an error handling in here?
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find a value for the attribute (" + attribKey + ")");
            }
            throw new Exception(e.getCause());
        }
    }

    static String getCredentialAttribute(final Set<Object> credentials, final String attribKey, final boolean bindWithAccessToken, final String msg) throws PrivilegedActionException {
        // Since this is only for jaxrs client internal usage, it's OK to override java2 security
        Object obj = AccessController.doPrivileged(
                new PrivilegedExceptionAction<Object>() {
                    @Override
                    public Object run() throws Exception {
                        int iCnt = 0;
                        for (Object credentialObj : credentials) {
                            iCnt++;
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, msg + "(" + iCnt + ") class:" + credentialObj.getClass().getName());
                            }
                            if (credentialObj instanceof Map) {
                                if (bindWithAccessToken) {
                                    Object accessToken = ((Map<?, ?>) credentialObj).get("access_token");
                                    if (accessToken == null) {
                                        continue; // on credentialObj
                                    }
                                }
                                Object value = ((Map<?, ?>) credentialObj).get(attribKey);
                                if (value != null) {
                                    return value;
                                }
                            }
                        }
                        return null;
                    }
                });
        if (obj != null) {
            return obj.toString();
        } else {
            return null;
        }
    }

    /**
     * Authenticate the username, create it's Subject and push it on to the thread.
     * It's up to the caller to save off the prior subject and make sure it gets restored,
     * and guard against any threading issues.
     * 
     * @param username
     * @return true if successful
     */
    public static synchronized boolean pushSubject(String username) {
        if (securityService == null || username == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning false because user or securityService is null,"
                        + " user= " + username + " secsvc= " + securityService);
            }
            return false;
        }
        AuthenticationService authenticationService = securityService.getAuthenticationService();
        Subject tempSubject = new Subject();

        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        if (!authenticationService.isAllowHashTableLoginWithIdOnly()) {
            hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        }
        hashtable.put("com.ibm.wsspi.security.cred.userId", username);
        tempSubject.getPublicCredentials().add(hashtable);
        try {
            Subject new_subject = authenticationService.authenticate(JaasLoginConfigConstants.SYSTEM_WEB_INBOUND, tempSubject);
            return setRunAsSubject(new_subject);

        } catch (AuthenticationException e) {
            FFDCFilter.processException(e,
                    TokenPropagationHelper.class.getName(), "pushSubject",
                    new Object[] { username });
            Tr.error(tc, "ERROR_AUTHENTICATE", new Object[] { e }); // CWWKS6103E
            return false;
        } catch (Exception e) {
            FFDCFilter.processException(e,
                    TokenPropagationHelper.class.getName(), "pushSubject",
                    new Object[] { username });
            return false;
        }
    }

    /**
     * set the runAsSubject. Contain any exceptions with FFDC.
     * 
     * @param subj
     *            - the subject to push onto the thread.
     * @return true if successful
     */
    public static synchronized boolean setRunAsSubject(Subject subj) {
        Subject before = null;
        try {
            before = getRunAsSubject();
            final Subject fsubj = subj;
            AccessController.doPrivileged(
                    new PrivilegedExceptionAction<Object>() {
                        @Override
                        public Object run() throws Exception {
                            WSSubject.setRunAsSubject(fsubj);
                            return null;
                        }
                    });

        } catch (PrivilegedActionException e) {
            FFDCFilter.processException(e,
                    TokenPropagationHelper.class.getName(), "setRunAsSubject",
                    new Object[] {});
            return false;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setRunAsSubject, runAsSubject before = ", before);
            Tr.debug(tc, "setRunAsSubject, runAsSubject after = ", subj);
        }
        return true;

    }

}
