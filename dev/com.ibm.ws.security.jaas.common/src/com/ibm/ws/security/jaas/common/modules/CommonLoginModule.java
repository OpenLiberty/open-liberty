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
package com.ibm.ws.security.jaas.common.modules;

import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.wsspi.security.auth.callback.Constants;

/**
 * Common login module logic needed by all WAS login modules.
 */
public abstract class CommonLoginModule implements LoginModule {

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, ?> options;
    public Subject temporarySubject;

    /**
     * {@inheritDoc} Common initialization of login modules.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
        this.subject = subject;
        this.sharedState = (Map<String, Object>) sharedState;
        this.options = options;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        cleanup();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        cleanup();
        return true;
    }

    private void cleanup() {
        cleanUpSubject();
        if (subject != null && !subject.isReadOnly()) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Set<Principal> principals = subject.getPrincipals();
                    principals.removeAll(subject.getPrincipals());
                    Set<Object> publicCredentials = subject.getPublicCredentials();
                    publicCredentials.removeAll(subject.getPublicCredentials());
                    Set<Object> privateCredentials = subject.getPrivateCredentials();
                    privateCredentials.removeAll(subject.getPrivateCredentials());
                    return null;
                }
            });
            subject = null;
        }
    }

    /**
     * Common Subject clean up.
     */
    public void cleanUpSubject() {
        if (temporarySubject != null) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    removeSubjectPrincipals();
                    removeSubjectPublicCredentials();
                    removeSubjectPrivateCredentials();
                    return null;
                }
            });
        }

        temporarySubject = null;
    }

    private void removeSubjectPrincipals() {
        Set<Principal> principals = subject.getPrincipals();
        principals.removeAll(temporarySubject.getPrincipals());
        Object toRemove = sharedState.get(Constants.WSPRINCIPAL_KEY);
        if (toRemove != null)
            principals.remove(toRemove);
    }

    private void removeSubjectPublicCredentials() {
        Set<Object> publicCredentials = subject.getPublicCredentials();
        publicCredentials.removeAll(temporarySubject.getPublicCredentials());
        Object toRemove = sharedState.get(Constants.WSCREDENTIAL_KEY);
        if (toRemove != null)
            publicCredentials.remove(toRemove);
    }

    private void removeSubjectPrivateCredentials() {
        Set<Object> privateCredentials = subject.getPrivateCredentials();
        privateCredentials.removeAll(temporarySubject.getPrivateCredentials());
        Object toRemove = sharedState.get(Constants.WSSSOTOKEN_KEY);
        if (toRemove != null)
            privateCredentials.remove(toRemove);
    }

    protected void setUpSubject(final Subject authSubj) throws LoginException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    subject.getPrincipals().addAll(authSubj.getPrincipals());
                    subject.getPublicCredentials().addAll(authSubj.getPublicCredentials());
                    subject.getPrivateCredentials().addAll(authSubj.getPrivateCredentials());
                    return null;
                }
            });
        } catch (PrivilegedActionException e) {
            throw new LoginException(e.getLocalizedMessage());
        }
    }

    protected void setAlreadyProcessed() {
        if (sharedState != null)
            sharedState.put(Constants.ALREADY_PROCESSED, "true");
    }
}
