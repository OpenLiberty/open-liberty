/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.callback;

import static com.ibm.ws.security.authentication.AuthenticationData.CERTCHAIN;
import static com.ibm.ws.security.authentication.AuthenticationData.HTTP_SERVLET_REQUEST;
import static com.ibm.ws.security.authentication.AuthenticationData.HTTP_SERVLET_RESPONSE;
import static com.ibm.ws.security.authentication.AuthenticationData.PASSWORD;
import static com.ibm.ws.security.authentication.AuthenticationData.REALM;
import static com.ibm.ws.security.authentication.AuthenticationData.TOKEN;
import static com.ibm.ws.security.authentication.AuthenticationData.TOKEN64;
import static com.ibm.ws.security.authentication.AuthenticationData.USERNAME;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 *
 */
public class AuthenticationDataCallbackHandler implements CallbackHandler {

    private final AuthenticationData authenticationData;

    /**
     * @param authenticationData
     */
    public AuthenticationDataCallbackHandler(AuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof NameCallback) {
                String name = (String) authenticationData.get(USERNAME);
                ((NameCallback) callback).setName(name);
            } else if (callback instanceof PasswordCallback) {
                char[] password = (char[]) authenticationData.get(PASSWORD);
                ((PasswordCallback) callback).setPassword(password);
            } else if (callback instanceof TokenCallback) {
                byte[] token = null;
                String token64 = (String) authenticationData.get(TOKEN64);
                if (token64 != null) {
                    token = Base64Coder.base64DecodeString(token64);
                } else {
                    token = (byte[]) authenticationData.get(TOKEN);
                }
                ((TokenCallback) callback).setToken(token);
            } else if (callback instanceof WSX509CertificateChainCallback) {
                X509Certificate[] certChain = (X509Certificate[]) authenticationData.get(CERTCHAIN);
                ((WSX509CertificateChainCallback) callback).setX509CertificateChain(certChain);
            } else if (callback instanceof WSRealmNameCallbackImpl) {
                String realmName = (String) authenticationData.get(REALM);
                ((WSRealmNameCallbackImpl) callback).setRealmName(realmName);
            } else if (callback instanceof WSServletRequestCallback) {
                HttpServletRequest request = (HttpServletRequest) authenticationData.get(HTTP_SERVLET_REQUEST);
                ((WSServletRequestCallback) callback).setHttpServletRequest(request);
            } else if (callback instanceof WSServletResponseCallback) {
                HttpServletResponse response = (HttpServletResponse) authenticationData.get(HTTP_SERVLET_RESPONSE);
                ((WSServletResponseCallback) callback).setHttpServletResponse(response);
            } else if (callback instanceof WSAppContextCallback) {
                Map context = (Map) authenticationData.get(AuthenticationData.APPLICATION_CONTEXT);
                ((WSAppContextCallback) callback).setContext(context);
            } else if (callback instanceof WSCredTokenCallbackImpl) {
                byte[] credToken = null;
                credToken = (byte[]) authenticationData.get(AuthenticationData.TOKEN);
                if (credToken != null) {
                    ((WSCredTokenCallbackImpl) callback).setCredToken(credToken);
                } else {
                    String credToken64 = (String) authenticationData.get(TOKEN64);
                    if (credToken64 != null) {
                        credToken = Base64Coder.base64DecodeString(credToken64);
                        ((WSCredTokenCallbackImpl) callback).setCredToken(credToken);
                    }
                }
            } else if (callback instanceof WSAuthMechOidCallbackImpl) {
                String authMechOID = (String) authenticationData.get(AuthenticationData.AUTHENTICATION_MECH_OID);
                ((WSAuthMechOidCallbackImpl) callback).setAuthMechOid(authMechOID);
            }
        }
    }
}
