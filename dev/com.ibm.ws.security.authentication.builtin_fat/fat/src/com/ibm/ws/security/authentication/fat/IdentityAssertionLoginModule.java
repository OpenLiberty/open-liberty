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

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.principals.WSPrincipal;

/**
 * Custom login module that adds another PublicCredential to the subject
 */
@SuppressWarnings("unchecked")
public class IdentityAssertionLoginModule implements LoginModule {

    private static final TraceComponent tc = Tr.register(IdentityAssertionLoginModule.class);
    protected Map<String, ?> _sharedState;
    protected Map<String, ?> _options;
    private final HashMap trustState = new HashMap();
    private final String managerUser = "LDAPUser2";

    /**
     * Initialization of login module
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        _sharedState = sharedState;
        _options = options;
        Tr.debug(tc, "FAT login module: initialize()");
    }

    public boolean login() throws LoginException {
        Tr.debug(tc, "FAT login module: login()");

        try {
            WSPrincipal principal = new WSPrincipal(managerUser, managerUser, WSPrincipal.AUTH_METHOD_IDENTITY_ASSERTION);
            trustState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.trusted", true);
            trustState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.principal", principal);
            Map<String, java.util.HashMap> mySharedState = (Map<String, java.util.HashMap>) _sharedState;
            mySharedState.put("com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule.state", trustState);
        } catch (Exception e) {
            throw new LoginException("LoginException: " + e.getMessage());
        }

        return true;
    }

    public boolean commit() throws LoginException {
        return true;
    }

    public boolean abort() {
        return true;
    }

    public boolean logout() {
        return true;
    }

}
