/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package loginmodule;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSName;

import com.ibm.wsspi.security.auth.callback.WSManagedConnectionFactoryCallback;
import com.ibm.wsspi.security.auth.callback.WSMappingPropertiesCallback;

public class TestGSSLoginModule implements LoginModule {

    public CallbackHandler callbackHandler;
    public Subject subject;
    public Map<String, Object> sharedState;
    public Map<String, ?> options;

    /** {@inheritDoc} */
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
    public boolean login() throws LoginException {

        try {
            Callback[] callbacks = getHandledCallbacks();
            setPasswordCredentialInSubject(callbacks);
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }

        return true;
    }

    private Callback[] getHandledCallbacks() throws IOException, UnsupportedCallbackException {
        Callback callbacks[] = new Callback[2];
        callbacks[0] = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
        callbacks[1] = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
        callbackHandler.handle(callbacks);
        return callbacks;
    }

    private void setPasswordCredentialInSubject(Callback[] callbacks) throws GSSException {
        GSSName name = new TestGSSName("dbuser1");
        GSSCredential credential = new TestGSSCredential(name);
        subject.getPrivateCredentials().add(credential);
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        return true;
    }

}
