/*******************************************************************************n * Copyright (c) 2021 IBM Corporation and others.n * All rights reserved. This program and the accompanying materialsn * are made available under the terms of the Eclipse Public License 2.0n * which accompanies this distribution, and is available atn * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0n *n * Contributors:n *     IBM Corporation - initial API and implementationn *******************************************************************************/
package com.ibm.ws.ejbcontainer.injection.mix.ejbint;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.wsspi.security.auth.callback.WSManagedConnectionFactoryCallback;
import com.ibm.wsspi.security.auth.callback.WSMappingPropertiesCallback;

/**
 * // *
 */
public class InjectionMixLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;

    public static boolean annCustomLoginModuleLogin = false;
    public static boolean xmlCustomLoginModuleLogin = false;

    /*
     * (non-Javadoc)
     *
     * @see javax.security.auth.spi.LoginModule#initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, java.util.Map, java.util.Map)
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = sharedState;
        this.options = options;

        System.out.println("InjectionMixLoginModule: Initialize");
        System.out.println(subject.toString());
        System.out.println(callbackHandler.toString());
        System.out.println(sharedState.toString());
        System.out.println(options.toString());
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.security.auth.spi.LoginModule#login()
     */
    @Override
    public boolean login() throws LoginException {
        System.out.println("InjectionMixLoginModule: login");

        try {
            final WSManagedConnectionFactoryCallback mcfCallback = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
            WSMappingPropertiesCallback mpropsCallback = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
            callbackHandler.handle(new Callback[] { mcfCallback, mpropsCallback });

            System.out.println(mpropsCallback.getProperties().toString());
            if (mpropsCallback.getProperties().toString().contains("BOB"))
                annCustomLoginModuleLogin = true;
            if (mpropsCallback.getProperties().toString().contains("RYAN"))
                xmlCustomLoginModuleLogin = true;
        } catch (Exception x) {
            throw (LoginException) new LoginException(x.getMessage()).initCause(x);
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        System.out.println("InjectionMixLoginModule: abort");
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        System.out.println("InjectionMixLoginModule: commit");
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        System.out.println("InjectionMixLoginModule: logout");
        return true;
    }

}
