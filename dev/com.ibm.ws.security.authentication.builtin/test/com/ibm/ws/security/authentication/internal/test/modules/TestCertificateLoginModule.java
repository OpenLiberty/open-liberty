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
package com.ibm.ws.security.authentication.internal.test.modules;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.ws.security.registry.LDAPUtils;
import com.ibm.wsspi.security.auth.callback.WSX509CertificateChainCallback;

/**
 *
 */
public class TestCertificateLoginModule implements LoginModule {

    private CallbackHandler callbackHandler;

    /** {@inheritDoc} */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> arg2, Map<String, ?> arg3) {
        this.callbackHandler = callbackHandler;
    }

    /** {@inheritDoc} */
    @Override
    public boolean login() throws LoginException {
        boolean succeeded = false;
        Callback[] callbacks;
        try {
            callbacks = getCallbacks(callbackHandler);
            X509Certificate[] certs = ((WSX509CertificateChainCallback) callbacks[0]).getX509CertificateChain();
            String username = LDAPUtils.getCNFromDN(certs[0].getSubjectX500Principal().getName());
            if ("testuser".equalsIgnoreCase(username)) {
                succeeded = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return succeeded;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        // TODO Auto-generated method stub
        return false;
    }

    private Callback[] getCallbacks(CallbackHandler callbackHandler) throws IOException, UnsupportedCallbackException {
        Callback[] callbacks = new Callback[1];
        callbacks[0] = new WSX509CertificateChainCallback(null);

        callbackHandler.handle(callbacks);
        return callbacks;
    }
}