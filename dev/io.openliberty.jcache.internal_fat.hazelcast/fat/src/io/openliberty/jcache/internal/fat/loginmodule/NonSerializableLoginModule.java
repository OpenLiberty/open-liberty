/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.jcache.internal.fat.loginmodule;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * Custom login module that adds a non-Serializable custom principal to the subject.
 */
public class NonSerializableLoginModule implements LoginModule {

    protected Map<String, ?> _sharedState;
    protected Subject _subject = null;
    protected CallbackHandler _callbackHandler;

    /**
     * Initialization of login module
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        _sharedState = sharedState;
        _subject = subject;
        _callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        System.out.println("Adding custom non-Serializable principal to subject.");

        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                _subject.getPrincipals().add(new NonSerializablePrincipal());
                return true;
            }
        });
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean abort() {
        cleanup();
        return true;
    }

    @Override
    public boolean logout() {
        cleanup();
        return true;
    }

    /**
     * Clears the subject
     */
    private void cleanup() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                _subject.getPrincipals().clear();
                _subject.getPublicCredentials().clear();
                _subject.getPrivateCredentials().clear();
                return null;
            }
        });
    }
}
