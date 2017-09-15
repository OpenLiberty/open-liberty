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
package com.ibm.ws.security.javaeesec;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.ProtectionPolicy;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.security.enterprise.AuthenticationException;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/*
 * This JASPI authentication module is used as the bridge ServerAuthModule for JSR-375.
 */
public class AuthModule implements ServerAuthModule {

    private static final TraceComponent tc = Tr.register(AuthModule.class);

    private static Class[] supportedMessageTypes = new Class[] { HttpServletRequest.class, HttpServletResponse.class };

    private MessagePolicy requestPolicy;
    private CallbackHandler handler;
    private Map<String, String> options;

    @Override
    public Class[] getSupportedMessageTypes() {
        return supportedMessageTypes;
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy, CallbackHandler handler, Map options) throws AuthException {
        this.requestPolicy = requestPolicy;
        this.handler = handler;
        this.options = new HashMap<String, String>();
        if (options != null) {
            this.options.putAll(options);
        }

        if (tc.isDebugEnabled()) {
            if (requestPolicy != null && requestPolicy.getTargetPolicies() != null) {
                for (TargetPolicy target : requestPolicy.getTargetPolicies()) {
                    ProtectionPolicy protectionPolicy = target.getProtectionPolicy();

                    if (protectionPolicy != null) {
                        Tr.debug(tc, "Target request ProtectionPolicy=" + protectionPolicy.getID());
                    }
                }
            }
        }

    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        AuthStatus status = AuthStatus.SEND_FAILURE;

        try {
            HttpAuthenticationMechanism authMech = getHttpAuthenticationMechanism();
            HttpMessageContext httpMessageContext = createHttpMessageContext(messageInfo, clientSubject);
            AuthenticationStatus authenticationStatus = authMech.validateRequest((HttpServletRequest) messageInfo.getRequestMessage(),
                                                                                 (HttpServletResponse) messageInfo.getResponseMessage(),
                                                                                 httpMessageContext);
            status = translateValidateRequestStatus(authenticationStatus);
//            if (authenticationStatus.equals(AuthenticationStatus.SUCCESS)) {
//                populateSubject(httpMessageContext, clientSubject);
//            }
        } catch (Exception e) {
            // TODO: Issue serviceability message.
            e.printStackTrace();
            AuthException authException = new AuthException();
            authException.initCause(e);
            throw authException;
        }
        return status;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        AuthStatus status = AuthStatus.SEND_FAILURE;
        // TODO: Determine if HttpMessageContext and HttpAuthenticationMechanism must have been cached in the MessageInfo
        try {
            HttpAuthenticationMechanism authMech = getHttpAuthenticationMechanism();
            HttpMessageContext httpMessageContext = createHttpMessageContext(messageInfo, null);
            AuthenticationStatus authenticationStatus = authMech.secureResponse((HttpServletRequest) messageInfo.getRequestMessage(),
                                                                                (HttpServletResponse) messageInfo.getResponseMessage(),
                                                                                httpMessageContext);
            status = translateSecureResponseStatus(authenticationStatus);
        } catch (AuthenticationException e) {
            // TODO: Issue serviceability message.
            e.printStackTrace();
            AuthException authException = new AuthException();
            authException.initCause(e);
            throw authException;
        }
        return status;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        // TODO: Call HttpAuthenticationMechanism's cleanSubject.
    }

    private HttpAuthenticationMechanism getHttpAuthenticationMechanism() {
        Instance<HttpAuthenticationMechanism> beanInstance = getCDI().select(HttpAuthenticationMechanism.class);
        return beanInstance.get();
    }

    protected CDI getCDI() {
        return CDI.current();
    }

    private HttpMessageContext createHttpMessageContext(MessageInfo messageInfo, Subject clientSubject) {
        HttpMessageContextImpl httpMessageContext = new HttpMessageContextImpl(messageInfo, clientSubject, handler);
        return httpMessageContext;
    }

    private void populateSubject(HttpMessageContext httpMessageContext, Subject clientSubject) {
        // TODO: Get subject information from the HttpMessageContext
        Hashtable<String, Object> cred = new Hashtable<String, Object>();
        cred.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, "JSR375:12345");
        cred.put(AttributeNameConstants.WSCREDENTIAL_USERID, "jaspiuser1");
        cred.put(AttributeNameConstants.WSCREDENTIAL_PASSWORD, "s3cur1ty");
        clientSubject.getPrivateCredentials().add(cred);
    }

    private AuthStatus translateValidateRequestStatus(AuthenticationStatus authenticationStatus) {
        AuthStatus status = AuthStatus.SEND_FAILURE;
        if (AuthenticationStatus.SUCCESS.equals(authenticationStatus)) {
            status = AuthStatus.SUCCESS;
        } else {
            status = translateCommon(authenticationStatus);
        }
        return status;
    }

    private AuthStatus translateSecureResponseStatus(AuthenticationStatus authenticationStatus) {
        AuthStatus status = AuthStatus.SEND_FAILURE;
        if (AuthenticationStatus.SUCCESS.equals(authenticationStatus)) {
            status = AuthStatus.SEND_SUCCESS;
        } else {
            status = translateCommon(authenticationStatus);
        }
        return status;
    }

    @Trivial
    private AuthStatus translateCommon(AuthenticationStatus authenticationStatus) {
        AuthStatus status = AuthStatus.SEND_FAILURE;
        if (AuthenticationStatus.SEND_FAILURE.equals(authenticationStatus)) {
            status = AuthStatus.SEND_FAILURE;
        } else if (AuthenticationStatus.SEND_CONTINUE.equals(authenticationStatus)) {
            status = AuthStatus.SEND_CONTINUE;
        }
        return status;
    }
}
