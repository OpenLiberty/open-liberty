/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.security;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * This LoginModule is a proxy for other login modules. The JAAS infrastructure
 * in the JDK uses Class.forName with the thread context class loader, which
 * inserts the actual login module class in the initiating loader table. Since
 * we want to support dynamic disabling/re-enabling of security bundles, we use
 * this proxy class because it is loaded by the kernel class loader, which lasts
 * for the duration of the JVM.
 */

public class LoginModuleProxy implements LoginModule {
    private LoginModule loginModule;
    public static final String KERNEL_DELEGATE = "kernelDelegate";

    public LoginModuleProxy() {}

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                           Map<String, ?> sharedState, Map<String, ?> options) {
        Class<?> target = (Class<?>) options.get(KERNEL_DELEGATE);
        try {
            loginModule = (LoginModule) target.newInstance();
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e.getMessage());
        } catch (InstantiationException e) {
            throw new RuntimeException(e.getMessage());
        }
        loginModule.initialize(subject, callbackHandler, sharedState, options);
    }

    @Override
    public boolean login() throws LoginException {
        return loginModule.login();
    }

    @Override
    public boolean commit() throws LoginException {
        return loginModule.commit();
    }

    @Override
    public boolean abort() throws LoginException {
        return loginModule.abort();
    }

    @Override
    public boolean logout() throws LoginException {
        return loginModule.logout();
    }
}
