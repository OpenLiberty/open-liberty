/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.jca;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebSphereRuntimePermission;
import com.ibm.websphere.security.auth.data.AuthData;
import com.ibm.websphere.security.auth.data.AuthDataProvider;
import com.ibm.wsspi.security.auth.callback.WSManagedConnectionFactoryCallback;
import com.ibm.wsspi.security.auth.callback.WSMappingPropertiesCallback;

/**
 * This login module handles authData based authentication.
 */
public class WSPrincipalMappingLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(WSPrincipalMappingLoginModule.class);

    private final static WebSphereRuntimePermission GET_PASSWORD_CREDENTIAL_PERMISSION = new WebSphereRuntimePermission("getPasswordCredential");

    private CallbackHandler callbackHandler;
    private Subject subject;
    private Subject temporarySubject;
    private boolean succeeded = false;

    /** {@inheritDoc} */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
        this.subject = subject;
    }

    /** {@inheritDoc} */
    @Override
    public boolean login() throws LoginException {
        try {
            Callback[] callbacks = getHandledCallbacks();
            setPasswordCredentialInTemporarySubject(callbacks);
        } catch (Exception e) {
            throw new LoginException(e.getMessage());
        }

        return succeeded;
    }

    private Callback[] getHandledCallbacks() throws IOException, UnsupportedCallbackException {
        Callback callbacks[] = new Callback[2];
        callbacks[0] = new WSManagedConnectionFactoryCallback("Target ManagedConnectionFactory: ");
        callbacks[1] = new WSMappingPropertiesCallback("Mapping Properties (HashMap): ");
        callbackHandler.handle(callbacks);
        return callbacks;
    }

    private void setPasswordCredentialInTemporarySubject(Callback[] callbacks) throws Exception {
        String alias = getAlias(callbacks);
        if (alias != null) {
            validateCallerHasPermission();
            setupTemporarySubject(callbacks, alias);
        }
    }

    @SuppressWarnings("rawtypes")
    private String getAlias(Callback[] callbacks) {
        String alias = null;
        Map properties = ((WSMappingPropertiesCallback) callbacks[1]).getProperties();

        if (properties != null) {
            alias = (String) properties.get(com.ibm.wsspi.security.auth.callback.Constants.MAPPING_ALIAS);
            if (alias != null) {
                alias = alias.trim();
            } else {
                Tr.error(tc, "MISSING_MAPPING_ALIAS_IN_CALLBACK_WSMAPPINGCALLBAKHANDLER");
            }
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
            Tr.error(tc, "MISSING_MAP_IN_CALLBACK_WSMAPPINGCALLBAKHANDLER");
        }

        return alias;
    }

    private void validateCallerHasPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_PASSWORD_CREDENTIAL_PERMISSION); // TODO: Investigate why LoginContext is performing the login in a doPriv block.
        }
    }

    private void setupTemporarySubject(Callback[] callbacks, String alias) throws LoginException {
        temporarySubject = new Subject();
        AuthData authData = AuthDataProvider.getAuthData(alias);
        ManagedConnectionFactory managedConnectionFactory = ((WSManagedConnectionFactoryCallback) callbacks[0]).getManagedConnectionFacotry();
        PasswordCredential passwordCredential = new PasswordCredential(authData.getUserName(), authData.getPassword());
        passwordCredential.setManagedConnectionFactory(managedConnectionFactory);
        temporarySubject.getPrivateCredentials().add(passwordCredential);
        succeeded = true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            return false;
        }

        setUpSubject();
        return true;
    }

    private void setUpSubject() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                updateSubjectWithTemporarySubjectContents();
                return null;
            }
        });
    }

    private void updateSubjectWithTemporarySubjectContents() {
        Set<Object> privateCredentials = temporarySubject.getPrivateCredentials();
        subject.getPrivateCredentials().addAll(privateCredentials);
    }

    /** {@inheritDoc} */
    @Override
    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        }
        cleanUpSubject();
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean logout() throws LoginException {
        cleanUpSubject();
        return true;
    }

    private void cleanUpSubject() {
        if (temporarySubject != null) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    removeSubjectPrivateCredentials();
                    return null;
                }
            });
        }

        temporarySubject = null;
    }

    private void removeSubjectPrivateCredentials() {
        Set<Object> privateCredentials = subject.getPrivateCredentials();
        privateCredentials.removeAll(temporarySubject.getPrivateCredentials());
    }

}
