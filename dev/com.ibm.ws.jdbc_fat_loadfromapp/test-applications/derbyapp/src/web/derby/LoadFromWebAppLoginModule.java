/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package web.derby;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.wsspi.security.auth.callback.WSManagedConnectionFactoryCallback;
import com.ibm.wsspi.security.auth.callback.WSMappingPropertiesCallback;

/**
 * This login module always assigns the same user/password, which tests can check for.
 */
public class LoadFromWebAppLoginModule implements LoginModule {
    private CallbackHandler callbackHandler;
    private Subject subject;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
        this.subject = subject;
    }

    @Override
    public boolean login() throws LoginException {
        try {
            final WSManagedConnectionFactoryCallback mcfCallback = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
            WSMappingPropertiesCallback mpropsCallback = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
            callbackHandler.handle(new Callback[] { mcfCallback, mpropsCallback });

            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                @Override
                public Void run() {
                    PasswordCredential passwordCredential = new PasswordCredential("webappuser", "webapppwd".toCharArray());
                    passwordCredential.setManagedConnectionFactory(mcfCallback.getManagedConnectionFacotry());
                    subject.getPrivateCredentials().add(passwordCredential);
                    return null;
                }
            });
        } catch (Exception x) {
            throw (LoginException) new LoginException(x.getMessage()).initCause(x);
        }

        return true;
    }

    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        return true;
    }
}
