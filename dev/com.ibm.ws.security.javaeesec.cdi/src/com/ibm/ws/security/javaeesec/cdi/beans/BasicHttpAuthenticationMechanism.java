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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.credential.BasicAuthenticationCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
public class BasicHttpAuthenticationMechanism implements HttpAuthenticationMechanism {
    @Inject
    ModulePropertiesProvider mpp;

    private static final TraceComponent tc = Tr.register(BasicHttpAuthenticationMechanism.class);

    private String realmName = null;
    private final String DEFAULT_REALM = "defaultRealm";

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        setRealmName();
        Subject clientSubject = httpMessageContext.getClientSubject();
        String authHeader = httpMessageContext.getRequest().getHeader("Authorization");

        if (authHeader == null) {
            status = handleNoAuthorizationHeader(httpMessageContext);
        } else {
            status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
        }

        return status;
    }

    private void setRealmName() {
        if (mpp != null) {
            Properties props = mpp.getAuthMechProperties(BasicHttpAuthenticationMechanism.class);
            if (props != null) {
                realmName = (String)props.get(JavaEESecConstants.REALM_NAME);
            }
        }
        if (realmName == null || realmName.trim().isEmpty()) {
            Tr.warning(tc, "JAVAEESEC_WARNING_NO_REALM_NAME");
            realmName = DEFAULT_REALM;
        }
    }

    private AuthenticationStatus handleNoAuthorizationHeader(HttpMessageContext httpMessageContext) {
        AuthenticationStatus status;
        if (httpMessageContext.isAuthenticationRequest() == false && httpMessageContext.isProtected() == false) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
            }
            status = AuthenticationStatus.NOT_DONE;
        } else {
            status = setChallengeAuthorizationHeader(httpMessageContext);
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    private AuthenticationStatus setChallengeAuthorizationHeader(HttpMessageContext httpMessageContext) {
        HttpServletResponse rsp = httpMessageContext.getResponse();
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        httpMessageContext.getMessageInfo().getMap().put(AttributeNameConstants.WSCREDENTIAL_REALM, realmName);

        return AuthenticationStatus.SEND_CONTINUE;
    }

    @SuppressWarnings("unchecked")
    private AuthenticationStatus handleAuthorizationHeader(@Sensitive String authorizationHeader, Subject clientSubject,
                                                           HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        if (authorizationHeader.startsWith("Basic ")) {
            String encodedHeader = authorizationHeader.substring(6);
            String basicAuthHeader = decodeCookieString(encodedHeader);

            if (isAuthorizationHeaderValid(basicAuthHeader)) { // BasicAuthenticationCredential.isValid does not work
                BasicAuthenticationCredential basicAuthCredential = new BasicAuthenticationCredential(encodedHeader);
                status = validateUserAndPassword(clientSubject, basicAuthCredential, httpMessageContext);
                if (status == AuthenticationStatus.SUCCESS) {
                    httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "JASPI_AUTH");
                    rspStatus = HttpServletResponse.SC_OK;
                }
            }
        }
        httpMessageContext.getResponse().setStatus(rspStatus);
        return status;
    }

    @Sensitive
    private String decodeCookieString(@Sensitive String cookieString) {
        try {
            return Base64Coder.base64Decode(cookieString);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAuthorizationHeaderValid(@Sensitive String basicAuthHeader) {
        int index = -1;
        boolean isNotValid = basicAuthHeader == null || basicAuthHeader.isEmpty() || (index = basicAuthHeader.indexOf(':')) <= 0 || index == basicAuthHeader.length() - 1;
        return !isNotValid;
    }

    private AuthenticationStatus validateUserAndPassword(Subject clientSubject, @Sensitive BasicAuthenticationCredential credential,
                                                         HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        IdentityStoreHandler identityStoreHandler = getIdentityStoreHandler();
        if (identityStoreHandler != null) {
            status = validateWithIdentityStore(clientSubject, credential, identityStoreHandler, httpMessageContext);
        }
        if (identityStoreHandler == null || status == AuthenticationStatus.NOT_DONE) {
            // If an identity store is not available, fall back to the original user registry.
            status = validateWithUserRegistry(clientSubject, credential, httpMessageContext.getHandler());
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    private IdentityStoreHandler getIdentityStoreHandler() {
        IdentityStoreHandler identityStoreHandler = null;
        Instance<IdentityStoreHandler> storeHandlerInstance = getCDI().select(IdentityStoreHandler.class);
        if (storeHandlerInstance.isUnsatisfied() == false && storeHandlerInstance.isAmbiguous() == false) {
            identityStoreHandler = storeHandlerInstance.get();
        }
        return identityStoreHandler;
    }

    @SuppressWarnings("rawtypes")
    protected CDI getCDI() {
        return CDI.current();
    }

    private AuthenticationStatus validateWithIdentityStore(Subject clientSubject, @Sensitive BasicAuthenticationCredential credential, IdentityStoreHandler identityStoreHandler,
                                                           HttpMessageContext httpMessageContext) {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        CredentialValidationResult result = identityStoreHandler.validate(credential);
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            setLoginHashtable(clientSubject, result);
            status = AuthenticationStatus.SUCCESS;
        } else if (result.getStatus() == CredentialValidationResult.Status.NOT_VALIDATED) {
            status = AuthenticationStatus.NOT_DONE;
        }
        return status;
    }

    private void setLoginHashtable(Subject clientSubject, CredentialValidationResult result) {
        Hashtable<String, Object> subjectHashtable = getSubjectHashtable(clientSubject);
        String callerPrincipalName = result.getCallerPrincipal().getName();
        String callerUniqueId = result.getCallerUniqueId();
        String realm = result.getIdentityStoreId();
        realm = realm != null ? realm : realmName;
        String uniqueId = callerUniqueId != null ? callerUniqueId : callerPrincipalName;

        setCommonAttributes(subjectHashtable, realm, callerPrincipalName);
        setUniqueId(subjectHashtable, realm, uniqueId);
        setGroups(subjectHashtable, result.getCallerGroups());
    }

    private void setCommonAttributes(Hashtable<String, Object> subjectHashtable, String realm, String callerPrincipalName) {
        subjectHashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, callerPrincipalName);
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, callerPrincipalName);
    }

    private void setUniqueId(Hashtable<String, Object> subjectHashtable, String realm, String uniqueId) {
        subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, "user:" + realm + "/" + uniqueId);
    }

    private void setGroups(Hashtable<String, Object> subjectHashtable, Set<String> groups) {
        if (groups != null && !groups.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding groups found in an identitystore", groups);
            }
            subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>(groups));
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No group  found in an identitystore");
            }
            subjectHashtable.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>());
        }
    }

    private Hashtable<String, Object> getSubjectHashtable(final Subject clientSubject) {
        Hashtable<String, Object> subjectHashtable = getSubjectExistingHashtable(clientSubject);
        if (subjectHashtable == null) {
            subjectHashtable = createNewSubjectHashtable(clientSubject);
        }
        return subjectHashtable;
    }

    private Hashtable<String, Object> getSubjectExistingHashtable(final Subject clientSubject) {
        if (clientSubject == null) {
            return null;
        }

        PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public Hashtable<String, Object> run() {
                Set hashtables = clientSubject.getPrivateCredentials(Hashtable.class);
                if (hashtables == null || hashtables.isEmpty()) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Subject has no Hashtable with custom credentials, return null.");
                    }
                    return null;
                } else {
                    Hashtable hashtable = (Hashtable) hashtables.iterator().next();
                    return hashtable;
                }
            }
        };
        Hashtable<String, Object> cred = AccessController.doPrivileged(action);
        return cred;
    }

    private Hashtable<String, Object> createNewSubjectHashtable(final Subject clientSubject) {
        PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

            @Override
            public Hashtable<String, Object> run() {
                Hashtable<String, Object> newCred = new Hashtable<String, Object>();
                clientSubject.getPrivateCredentials().add(newCred);
                return newCred;
            }
        };
        return AccessController.doPrivileged(action);
    }

    private AuthenticationStatus validateWithUserRegistry(Subject clientSubject, @Sensitive BasicAuthenticationCredential credential,
                                                          CallbackHandler handler) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        if (handler != null) {
            PasswordValidationCallback pwcb = new PasswordValidationCallback(clientSubject, credential.getCaller(), credential.getPassword().getValue());
            try {
                handler.handle(new Callback[] { pwcb });
                boolean isValidPassword = pwcb.getResult();
                if (isValidPassword) {
                    status = AuthenticationStatus.SUCCESS;
                }
            } catch (Exception e) {
                throw new AuthenticationException(e.toString());
            }
        }
        return status;
    }

    // this is for unit test.
    protected void setMPP(ModulePropertiesProvider mpp) {
        this.mpp = mpp;
    }
}