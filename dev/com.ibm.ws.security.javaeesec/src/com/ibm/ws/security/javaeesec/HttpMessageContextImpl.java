/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 *
 */
public class HttpMessageContextImpl implements HttpMessageContext {

    private static final TraceComponent tc = Tr.register(HttpMessageContextImpl.class);

    private final MessageInfo messageInfo;
    private final Subject clientSubject;
    private final CallbackHandler handler;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Principal principal = null;
    private Set<String> groups = Collections.emptySet();
    private boolean isRegisterSession = false;
    private AuthenticationParameters authenticationParameters = new AuthenticationParameters();
    private boolean isAuthenticationRequest = false;

    private CredentialValidationResult result;

    /**
     * @param messageInfo
     * @param clientSubject
     * @param handler
     */
    public HttpMessageContextImpl(MessageInfo messageInfo, Subject clientSubject, CallbackHandler handler) {
        this.messageInfo = messageInfo;
        this.clientSubject = clientSubject;
        this.handler = handler;
        request = (HttpServletRequest) messageInfo.getRequestMessage();
        response = (HttpServletResponse) messageInfo.getResponseMessage();
    }

    /**
     * @param messageInfo
     * @param clientSubject
     * @param handler
     * @param authenticationParameters
     */
    public HttpMessageContextImpl(MessageInfo messageInfo, Subject clientSubject, CallbackHandler handler, AuthenticationParameters authenticationParameters) {
        this(messageInfo, clientSubject, handler);
        this.authenticationParameters = authenticationParameters;
        this.isAuthenticationRequest = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#cleanClientSubject()
     */
    @Override
    public void cleanClientSubject() {
        // The container will remove container specific contents according to JASPIC (JSR 196) for the ServerAuthModule#cleanSubject.
        // The mechanism or the message context wrapper must remove mechanism specific contents.
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#doNothing()
     */
    @Override
    public AuthenticationStatus doNothing() {
        return AuthenticationStatus.NOT_DONE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#forward(java.lang.String)
     */
    @Override
    public AuthenticationStatus forward(String path) {
        try {
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(path);
            requestDispatcher.forward(request, response);
        } catch (Exception e) {
            // TODO: Add serviceability message
        }
        return AuthenticationStatus.SEND_CONTINUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getAuthParameters()
     */
    @Override
    public AuthenticationParameters getAuthParameters() {
        return authenticationParameters;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getCallerPrincipal()
     */
    @Override
    public Principal getCallerPrincipal() {
        return principal;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getClientSubject()
     */
    @Override
    public Subject getClientSubject() {
        return clientSubject;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getGroups()
     */
    @Override
    public Set<String> getGroups() {
        return groups;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getHandler()
     */
    @Override
    public CallbackHandler getHandler() {
        return handler;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getMessageInfo()
     */
    @Override
    public MessageInfo getMessageInfo() {
        return messageInfo;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getRequest()
     */
    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#getResponse()
     */
    @Override
    public HttpServletResponse getResponse() {
        return response;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#isAuthenticationRequest()
     */
    @Override
    public boolean isAuthenticationRequest() {
        return isAuthenticationRequest;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#isProtected()
     */
    @Override
    public boolean isProtected() {
        return ((String) messageInfo.getMap().get("javax.security.auth.message.MessagePolicy.isMandatory")).equalsIgnoreCase("TRUE");
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#isRegisterSession()
     */
    @Override
    public boolean isRegisterSession() {
        return isRegisterSession;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#notifyContainerAboutLogin(javax.security.enterprise.identitystore.CredentialValidationResult)
     */
    @Override
    public AuthenticationStatus notifyContainerAboutLogin(CredentialValidationResult result) {
        if (CredentialValidationResult.Status.VALID.equals(result.getStatus())) {
            this.result = result;
            return notifyContainerAboutLogin(result.getCallerPrincipal(), result.getCallerGroups());
        }
        return AuthenticationStatus.SEND_FAILURE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#notifyContainerAboutLogin(java.security.Principal, java.util.Set)
     */
    @Override
    public AuthenticationStatus notifyContainerAboutLogin(Principal principal, Set<String> groups) {
        try {
            this.principal = principal;
            this.groups = Collections.unmodifiableSet(groups); // Unmodifiable view to avoid corruption

            Callback[] callbacks = new Callback[3];
            callbacks[0] = getRealmNameCallback();
            callbacks[1] = new CallerPrincipalCallback(clientSubject, principal);
            callbacks[2] = new GroupPrincipalCallback(clientSubject, groups.toArray(new String[] {}));
            handler.handle(callbacks);
        } catch (Exception e) {
            // TODO: Determine if this needs a serviceability message
        }
        return AuthenticationStatus.SUCCESS;
    }

    private NameCallback getRealmNameCallback() {
        NameCallback realmNameCallback = new NameCallback(AttributeNameConstants.WSCREDENTIAL_REALM);
        realmNameCallback.setName(getRealm());
        return realmNameCallback;
    }

    private String getRealm() {
        String realm = JavaEESecConstants.DEFAULT_REALM;
        if (result != null) {
            String idStoreId = result.getIdentityStoreId();
            if (idStoreId == null || idStoreId.trim().isEmpty()) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "The identity store id is not defined, \"defaultRealm\" is used.");
                }
            } else {
                realm = idStoreId;
            }
        }
        return realm;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#notifyContainerAboutLogin(java.lang.String, java.util.Set)
     */
    @Override
    public AuthenticationStatus notifyContainerAboutLogin(String callername, Set<String> groups) {
        try {
            this.groups = Collections.unmodifiableSet(groups); // Unmodifiable view to avoid corruption
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new CallerPrincipalCallback(clientSubject, callername);
            callbacks[1] = new GroupPrincipalCallback(clientSubject, groups.toArray(new String[] {}));
            handler.handle(callbacks);
        } catch (Exception e) {
            // TODO: Determine if this needs a serviceability message
        }
        return AuthenticationStatus.SUCCESS;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#redirect(java.lang.String)
     */
    @Override
    public AuthenticationStatus redirect(String location) {
        try {
            response.sendRedirect(response.encodeURL(location));
            response.setStatus(HttpServletResponse.SC_FOUND);
        } catch (IOException e) {
            // TODO: Determine if this needs a serviceability message
        }
        return AuthenticationStatus.SEND_CONTINUE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#responseNotFound()
     */
    @Override
    public AuthenticationStatus responseNotFound() {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return AuthenticationStatus.SEND_FAILURE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#responseUnauthorized()
     */
    @Override
    public AuthenticationStatus responseUnauthorized() {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return AuthenticationStatus.SEND_FAILURE;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#setRegisterSession(java.lang.String, java.util.Set)
     */
    @Override
    public void setRegisterSession(String callerName, Set<String> groups) {
        isRegisterSession = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#setRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#setResponse(javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.enterprise.authentication.mechanism.http.HttpMessageContext#withRequest(javax.servlet.http.HttpServletRequest)
     */
    @Override
    public HttpMessageContext withRequest(HttpServletRequest request) {
        this.request = request;
        return this;
    }

}
