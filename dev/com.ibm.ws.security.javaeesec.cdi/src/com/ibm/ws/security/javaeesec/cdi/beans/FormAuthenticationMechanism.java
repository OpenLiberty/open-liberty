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
import java.util.Map;
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
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

@Default
@ApplicationScoped
@LoginToContinue
public class FormAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final TraceComponent tc = Tr.register(FormAuthenticationMechanism.class);

    @Override
    public AuthenticationStatus validateRequest(HttpServletRequest request,
                                                HttpServletResponse response,
                                                HttpMessageContext httpMessageContext) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;

        Subject clientSubject = httpMessageContext.getClientSubject();
        @SuppressWarnings("unchecked")
        Map<String, String> msgMap = httpMessageContext.getMessageInfo().getMap();
        CallbackHandler handler = httpMessageContext.getHandler();
        HttpServletRequest req = httpMessageContext.getRequest();
        HttpServletResponse rsp = httpMessageContext.getResponse();
        String username = null;
        String password = null;
        // in order to preserve the post parameter, unless the target url is j_security_check, do not read
        // j_username and j_password.
        String method = req.getMethod();
        String uri = null;
        if ("POST".equalsIgnoreCase(method)) {
            uri = req.getRequestURI();
            if (uri.contains("/j_security_check")) {
                username = req.getParameter("j_username");
                password = req.getParameter("j_password");
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "method : " + method + ", URI : " + uri + ", j_username : " + username);
        }

        if (httpMessageContext.isAuthenticationRequest()) {
            if (username != null && password != null) {
                status = handleFormLogin(username, password, rsp, msgMap, clientSubject, handler);
            } else {
                status = AuthenticationStatus.SEND_CONTINUE;
            }
        } else {
            if (username == null || password == null) {
                if (httpMessageContext.isProtected() == false) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "both isAuthenticationRequest and isProtected returns false. returing NOT_DONE,");
                    }
                    status = AuthenticationStatus.NOT_DONE;
                } else {
                    status = AuthenticationStatus.SEND_CONTINUE;
                }
            } else {
                status = handleFormLogin(username, password, rsp, msgMap, clientSubject, handler);
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

    /**
     * note that both username and password should not be null.
     */

    private AuthenticationStatus handleFormLogin(String username, @Sensitive String password, HttpServletResponse rsp, Map<String, String> msgMap, Subject clientSubject,
                                                 CallbackHandler handler) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        int rspStatus = HttpServletResponse.SC_FORBIDDEN;
        UsernamePasswordCredential credential = new UsernamePasswordCredential(username, password);
        status = validateUserAndPassword(clientSubject, credential, handler);
        if (status == AuthenticationStatus.SUCCESS) {
            msgMap.put("javax.servlet.http.authType", "JASPI_AUTH");
            rspStatus = HttpServletResponse.SC_OK;
        } else {
            // TODO: Audit invalid user or password
        }
        rsp.setStatus(rspStatus);
        return status;
    }

    private AuthenticationStatus validateUserAndPassword(Subject clientSubject, @Sensitive UsernamePasswordCredential credential,
                                                         CallbackHandler handler) throws AuthenticationException {
        AuthenticationStatus status = AuthenticationStatus.SEND_FAILURE;
        IdentityStoreHandler identityStoreHandler = getIdentityStoreHandler();
        if (identityStoreHandler != null) {
            status = validateWithIdentityStore(clientSubject, credential, identityStoreHandler);
        } else {
            Tr.warning(tc, "JAVAEESEC_CDI_WARNING_NO_IDENTITY_STORE_HANDLER");
        }
        if (identityStoreHandler == null || status == AuthenticationStatus.NOT_DONE) {
            // If an identity store is not available, fall back to the original user registry.
            status = validateWithUserRegistry(clientSubject, credential, handler);
        }
        return status;
    }

    private AuthenticationStatus validateWithUserRegistry(Subject clientSubject, @Sensitive UsernamePasswordCredential credential,
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
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "A CallbackHandler object, which is required for validating user id and password, is null.");
            }
        }
        return status;
    }

    private AuthenticationStatus validateWithIdentityStore(Subject clientSubject, @Sensitive UsernamePasswordCredential credential,
                                                           IdentityStoreHandler identityStoreHandler) throws AuthenticationException {
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

    protected void createLoginHashMap(Subject clientSubject, CredentialValidationResult result) throws AuthenticationException {
        Utils.validateResult(result);
        Hashtable<String, Object> credData = getSubjectCustomData(clientSubject);
        Set<String> groups = result.getCallerGroups();
        String realm = result.getIdentityStoreId();
        if (realm == null) {
            realm = "default";
        }
        credData.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        credData.put(AttributeNameConstants.WSCREDENTIAL_USERID, result.getCallerPrincipal().getName());
        credData.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, result.getCallerUniqueId());
        credData.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, "user:" + realm + "/" + result.getCallerUniqueId());

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
        Instance<IdentityStoreHandler> storeHandlerInstance = getCDI().select(IdentityStoreHandler.class);
        if (storeHandlerInstance != null && storeHandlerInstance.isUnsatisfied() == false && storeHandlerInstance.isAmbiguous() == false) {
            identityStoreHandler = storeHandlerInstance.get();
        }
        return identityStoreHandler;
    }

    protected CDI getCDI() {
        return CDI.current();
    }

}
