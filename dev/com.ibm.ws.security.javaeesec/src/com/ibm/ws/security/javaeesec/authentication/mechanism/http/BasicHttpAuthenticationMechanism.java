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
package com.ibm.ws.security.javaeesec.authentication.mechanism.http;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
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
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
public class BasicHttpAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final TraceComponent tc = Tr.register(BasicHttpAuthenticationMechanism.class);

    private final String realmName;
    private final String DEFAULT_REALM = "defaultRealm";

    /**
     * @param realmName
     */
    public BasicHttpAuthenticationMechanism(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        String authHeader = httpMessageContext.getRequest().getHeader("Authorization");

        if (httpMessageContext.isAuthenticationRequest()) {
            if (authHeader == null) {
                status = setChallengeAuthorizationHeader(httpMessageContext.getResponse());
            } else {
                status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
            }
        } else {
            if (authHeader == null) {
                if (httpMessageContext.isProtected() == false) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
                    }
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    status = setChallengeAuthorizationHeader(httpMessageContext.getResponse());
                }
            } else {
                status = handleAuthorizationHeader(authHeader, clientSubject, httpMessageContext);
            }
        }

        return status;
    }

    @Override
    public AuthenticationStatus secureResponse(HttpServletRequest request,
                                               HttpServletResponse response,
                                               HttpMessageContext httpMessageContext) throws AuthenticationException {
        return AuthenticationStatus.SUCCESS;
    }

    @Override
    public void cleanSubject(HttpServletRequest request,
                             HttpServletResponse response,
                             HttpMessageContext httpMessageContext) {

    }

    private AuthenticationStatus setChallengeAuthorizationHeader(HttpServletResponse rsp) {
        rsp.setHeader("WWW-Authenticate", "Basic realm=\"" + realmName + "\"");
        rsp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
        return AuthenticationStatus.SEND_CONTINUE;
    }

    @SuppressWarnings("unchecked")
    private AuthenticationStatus handleAuthorizationHeader(String authHeader, Subject clientSubject, HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        if (authHeader.startsWith("Basic ")) {
            String encodedHeader = authHeader.substring(6);
            String basicAuthHeader = decodeCookieString(encodedHeader);

            if (isAuthorizationHeaderValid(basicAuthHeader)) { // BasicAuthenticationCredential.isValid does not work
                BasicAuthenticationCredential basicAuthCredential = new BasicAuthenticationCredential(encodedHeader);
                status = validateUserAndPassword(clientSubject, basicAuthCredential, httpMessageContext);
                if (status == AuthenticationStatus.SUCCESS) {
                    httpMessageContext.getMessageInfo().getMap().put("javax.servlet.http.authType", "JASPI_AUTH");
                    rspStatus = HttpServletResponse.SC_OK;
                } else {
                    // TODO: Audit invalid user or password
                }
            } else {
                // TODO: Determine if serviceability message is needed
            }
        } else {
            // TODO: Determine if serviceability message is needed
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

    private AuthenticationStatus validateWithIdentityStore(Subject clientSubject, @Sensitive BasicAuthenticationCredential credential, IdentityStoreHandler identityStoreHandler,
                                                           HttpMessageContext httpMessageContext) {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        CredentialValidationResult result = identityStoreHandler.validate(credential);
        if (result.getStatus() == CredentialValidationResult.Status.VALID) {
            createLoginHashMap(clientSubject, result);
            status = AuthenticationStatus.SUCCESS;
        } else if (result.getStatus() == CredentialValidationResult.Status.NOT_VALIDATED) {
            // TODO: error message. no identitystore.
            status = AuthenticationStatus.NOT_DONE;
        } else {
            // TODO: Audit invalid user or password
        }
        return status;
    }

    protected void createLoginHashMap(Subject clientSubject, CredentialValidationResult result) {
        Hashtable<String, Object> credData = getSubjectCustomData(clientSubject);
        Set<String> groups = result.getCallerGroups();
        String realm = result.getIdentityStoreId();
        if (realm == null) {
            if (realmName != null) {
                realm = realmName;
            } else {
                realm = DEFAULT_REALM;
            }
        }
        credData.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        credData.put(AttributeNameConstants.WSCREDENTIAL_USERID, result.getCallerPrincipal().getName());
        credData.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, result.getCallerUniqueId());

        credData.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        if (groups != null && !groups.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding groups found in an identitystore", groups);
            }
            credData.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>(groups));
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No group  found in an identitystore");
            }
            credData.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, new ArrayList<String>());
        }
        return;
    }

    protected Hashtable<String, Object> getSubjectCustomData(final Subject clientSubject) {
        Hashtable<String, Object> cred = getCustomCredentials(clientSubject);
        if (cred == null) {
            PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

                @Override
                public Hashtable<String, Object> run() {
                    Hashtable<String, Object> newCred = new Hashtable<String, Object>();
                    clientSubject.getPrivateCredentials().add(newCred);
                    return newCred;
                }
            };
            cred = AccessController.doPrivileged(action);
        }
        return cred;
    }

    protected Hashtable<String, Object> getCustomCredentials(final Subject clientSubject) {
        if (clientSubject == null)
            return null;
        PrivilegedAction<Hashtable<String, Object>> action = new PrivilegedAction<Hashtable<String, Object>>() {

            @SuppressWarnings("unchecked")
            @Override
            public Hashtable<String, Object> run() {
                Set s = clientSubject.getPrivateCredentials(Hashtable.class);
                if (s == null || s.isEmpty()) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Subject has no Hashtable with custom credentials, return null.");
                    return null;
                } else {
                    Hashtable t = (Hashtable) s.iterator().next();
                    return t;
                }
            }
        };
        Hashtable<String, Object> cred = AccessController.doPrivileged(action);
        return cred;
    }

    private IdentityStoreHandler getIdentityStoreHandler() {
        IdentityStoreHandler identityStoreHandler = null;
        Instance<IdentityStoreHandler> storeHandlerInstance = CDI.current().select(IdentityStoreHandler.class);
        if (storeHandlerInstance.isUnsatisfied() == false && storeHandlerInstance.isAmbiguous() == false) {
            identityStoreHandler = storeHandlerInstance.get();
        }
        return identityStoreHandler;
    }

}