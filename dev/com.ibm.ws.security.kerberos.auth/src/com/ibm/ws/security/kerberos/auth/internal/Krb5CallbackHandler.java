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
package com.ibm.ws.security.kerberos.auth.internal;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

@Trivial
public class Krb5CallbackHandler implements CallbackHandler {

    private static final TraceComponent tc = Tr.register(Krb5CallbackHandler.class);

    private final SerializableProtectedString pass;

    public Krb5CallbackHandler(SerializableProtectedString pass) {
        this.pass = pass;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (callbacks == null || callbacks.length == 0)
            return;

        for (Callback c : callbacks) {
            if (c instanceof PasswordCallback) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Setting kerbeos password on PasswordCallback");
                }
                PasswordCallback pc = (PasswordCallback) c;
                pc.setPassword(pass.getChars());
            }
        }
    }

}
