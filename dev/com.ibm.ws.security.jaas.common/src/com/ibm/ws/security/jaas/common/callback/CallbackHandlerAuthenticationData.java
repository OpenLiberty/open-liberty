/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 *
 */
public class CallbackHandlerAuthenticationData {
    private static final TraceComponent tc = Tr.register(CallbackHandlerAuthenticationData.class);

    private final CallbackHandler callbackHandler;;

    public CallbackHandlerAuthenticationData(CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    public AuthenticationData createAuthenticationData() throws IOException, UnsupportedCallbackException {
        AuthenticationData authenticationData = new WSAuthenticationData();

        Callback[] callbacks = getAllSupportedCallbacks(callbackHandler);

        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callbacks[i];
                authenticationData.set(AuthenticationData.USERNAME, nameCallback.getName());
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback passwordCallback = (PasswordCallback) callbacks[i];
                authenticationData.set(AuthenticationData.PASSWORD, passwordCallback.getPassword());
            } else if (callbacks[i] instanceof WSServletRequestCallback) {
                WSServletRequestCallback wsServletRequestCallback = (WSServletRequestCallback) callbacks[i];
                authenticationData.set(AuthenticationData.HTTP_SERVLET_REQUEST, wsServletRequestCallback.getHttpServletRequest());
            } else if (callbacks[i] instanceof WSServletResponseCallback) {
                WSServletResponseCallback wsServletResponseCallback = (WSServletResponseCallback) callbacks[i];
                authenticationData.set(AuthenticationData.HTTP_SERVLET_RESPONSE, wsServletResponseCallback.getHttpServletResponse());
            } else if (callbacks[i] instanceof WSAppContextCallback) {
                WSAppContextCallback wsAppContextCallback = (WSAppContextCallback) callbacks[i];
                authenticationData.set(AuthenticationData.APPLICATION_CONTEXT, wsAppContextCallback.getContext());
            } else if (callbacks[i] instanceof WSRealmNameCallbackImpl) {
                WSRealmNameCallbackImpl wsRealmNameCallback = (WSRealmNameCallbackImpl) callbacks[i];
                authenticationData.set(AuthenticationData.REALM, wsRealmNameCallback.getRealmName());
            } else if (callbacks[i] instanceof WSX509CertificateChainCallback) {
                WSX509CertificateChainCallback wsX509CertificateCallback = (WSX509CertificateChainCallback) callbacks[i];
                authenticationData.set(AuthenticationData.CERTCHAIN, wsX509CertificateCallback.getX509CertificateChain());
            } else if (callbacks[i] instanceof WSCredTokenCallbackImpl) {
                WSCredTokenCallbackImpl wsCredTokenCallbackImpl = (WSCredTokenCallbackImpl) callbacks[i];
                authenticationData.set(AuthenticationData.TOKEN, wsCredTokenCallbackImpl.getCredToken());
            } else if (callbacks[i] instanceof WSAuthMechOidCallbackImpl) {
                WSAuthMechOidCallbackImpl wsAuthMechOidCallback = (WSAuthMechOidCallbackImpl) callbacks[i];
                authenticationData.set(AuthenticationData.AUTHENTICATION_MECH_OID, wsAuthMechOidCallback.getAuthMechOid());
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The following callback was ignored: " + callbacks[i].getClass().getName());
                }
            }
        }
        return authenticationData;
    }

    Callback[] getAllSupportedCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
        Callback[] callbacks = new Callback[9];
        callbacks[0] = new NameCallback("Username: ");
        callbacks[1] = new PasswordCallback("Password: ", false);
        callbacks[2] = new WSServletRequestCallback("HttpServletRequest: ");
        callbacks[3] = new WSServletResponseCallback("HttpServletResponse: ");
        callbacks[4] = new WSAppContextCallback("ApplicationContextCallback: ");
        callbacks[5] = new WSRealmNameCallbackImpl("Realm Name:");
        callbacks[6] = new WSX509CertificateChainCallback("X509Certificate[]: ");
        callbacks[7] = new WSCredTokenCallbackImpl("Credential Token: ");
        callbacks[8] = new WSAuthMechOidCallbackImpl("AuthMechOid: ");
        callbackHandler.handle(callbacks);
        return callbacks;
    }
}
