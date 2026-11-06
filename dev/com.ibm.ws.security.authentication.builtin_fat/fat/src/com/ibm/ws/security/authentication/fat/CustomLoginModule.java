/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.fat;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.callback.WSAuthMechOidCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSCredTokenCallbackImpl;
import com.ibm.websphere.security.auth.callback.WSRealmNameCallbackImpl;
import com.ibm.wsspi.security.auth.callback.WSAppContextCallback;
import com.ibm.wsspi.security.auth.callback.WSServletRequestCallback;
import com.ibm.wsspi.security.auth.callback.WSServletResponseCallback;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 * Custom login module that adds another PublicCredential to the subject
 */
public class CustomLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(CustomLoginModule.class);
    protected Map<String, ?> _sharedState;
    protected Subject _subject = null;
    protected CallbackHandler _callbackHandler;
    private final String customPrivateCredential = "CustomLoginModuleCredential";

    /**
     * Initialization of login module
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        _sharedState = sharedState;
        _subject = subject;
        _callbackHandler = callbackHandler;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "FAT login module: initialize()");
        }
    }

    public boolean login() throws LoginException {
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "FAT login module: login()");
            }
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                public Object run() throws Exception {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "FAT login module: add to Private Credentials: " + customPrivateCredential);
                    }
                    _subject.getPrivateCredentials().add(customPrivateCredential);
                    return null;
                }

            });
        } catch (PrivilegedActionException e) {
            throw new LoginException(e.getLocalizedMessage());
        }

        String username = null;
        char passwordChar[] = null;
        byte[] credToken = null;
        HttpServletRequest request = null;
        HttpServletResponse response = null;
        Map appContext = null;
        String realm = null;
        String authMechOid = null;
        java.security.cert.X509Certificate[] certChain = null;

        NameCallback nameCallback = null;
        PasswordCallback passwordCallback = null;
        WSCredTokenCallbackImpl wsCredTokenCallback = null;
        WSServletRequestCallback wsServletRequestCallback = null;
        WSServletResponseCallback wsServletResponseCallback = null;
        WSAppContextCallback wsAppContextCallback = null;
        WSRealmNameCallbackImpl wsRealmNameCallback = null;
        WSX509CertificateChainCallback wsX509CertificateCallback = null;
        WSAuthMechOidCallbackImpl wsAuthMechOidCallback = null;

        Callback[] callbacks = new Callback[9];
        callbacks[0] = nameCallback = new NameCallback("Username: ");
        callbacks[1] = passwordCallback = new PasswordCallback("Password: ", false);
        callbacks[2] = wsCredTokenCallback = new WSCredTokenCallbackImpl("Credential Token: ");
        callbacks[3] = wsServletRequestCallback = new WSServletRequestCallback("HttpServletRequest: ");
        callbacks[4] = wsServletResponseCallback = new WSServletResponseCallback("HttpServletResponse: ");
        callbacks[5] = wsAppContextCallback = new WSAppContextCallback("ApplicationContextCallback: ");
        callbacks[6] = wsRealmNameCallback = new WSRealmNameCallbackImpl("Realm name:");
        callbacks[7] = wsX509CertificateCallback = new WSX509CertificateChainCallback("X509Certificate[]: ");
        callbacks[8] = wsAuthMechOidCallback = new WSAuthMechOidCallbackImpl("AuthMechOid: ");

        try {
            _callbackHandler.handle(callbacks);
        } catch (Exception e) {
            // handle exception
        }

        if (nameCallback != null)
            username = nameCallback.getName();

        if (passwordCallback != null)
            passwordChar = passwordCallback.getPassword();

        if (wsCredTokenCallback != null)
            credToken = wsCredTokenCallback.getCredToken();

        if (wsServletRequestCallback != null)
            request = wsServletRequestCallback.getHttpServletRequest();

        if (wsServletResponseCallback != null)
            response = wsServletResponseCallback.getHttpServletResponse();

        if (wsAppContextCallback != null)
            appContext = wsAppContextCallback.getContext();

        if (wsRealmNameCallback != null)
            realm = wsRealmNameCallback.getRealmName();

        if (wsX509CertificateCallback != null)
            certChain = wsX509CertificateCallback.getX509CertificateChain();

        if (wsAuthMechOidCallback != null)
            authMechOid = wsAuthMechOidCallback.getAuthMechOid();

        String password = passwordChar != null ? String.valueOf(passwordChar) : "null";
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "username = " + username);
            Tr.debug(tc, "passwordChar = " + password);
            Tr.debug(tc, "realm = " + realm);
            Tr.debug(tc, "authMechOid = " + authMechOid.toString());
        }

        _subject.getPrivateCredentials().add("username = " + username);
        _subject.getPrivateCredentials().add("password = " + password);
        _subject.getPrivateCredentials().add("realm = " + realm);
        _subject.getPrivateCredentials().add("authMechOid = " + authMechOid.toString());
        _subject.getPrivateCredentials().add("request = " + (request != null ? "<not null>" : "<null>"));
        _subject.getPrivateCredentials().add("response = " + (response != null ? "<not null>" : "<null>"));
        _subject.getPrivateCredentials().add("certChain = " + (certChain != null ? "<not null>" : "<null>"));

        return true;
    }

    public boolean commit() throws LoginException {
        return true;
    }

    public boolean abort() {
        cleanup();
        return true;
    }

    public boolean logout() {
        cleanup();
        return true;
    }

    /**
     * Clears the subject
     */
    private void cleanup() {
        _subject.getPrivateCredentials().clear();
    }

}
