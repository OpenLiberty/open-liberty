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
package com.ibm.ws.security.javaeesec;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.MessagePolicy.ProtectionPolicy;
import javax.security.auth.message.MessagePolicy.Target;
import javax.security.auth.message.MessagePolicy.TargetPolicy;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 * JSR-375 ServerAuthContext for invoking the bridge ServeAuthModule.
 */
public class AuthContext implements ServerAuthContext {

    private final ServerAuthModule module;
    private final CallbackHandler handler;

    public AuthContext(String authContextID, Subject serviceSubject, Map properties, CallbackHandler handler) {
        boolean isMandatory = "JASPI_PROTECTED".equals(authContextID);
        this.handler = handler;
        module = getAuthModule(); // new AuthModule();
        Map<String, String> props = new HashMap<String, String>();

        if (properties != null) {
            props.putAll(properties);
        }

        try {
            MessagePolicy requestPolicy = newMessagePolicy(isMandatory, newProtectionPolicy(ProtectionPolicy.AUTHENTICATE_SENDER));
            MessagePolicy responsePolicy = null;
            module.initialize(requestPolicy, responsePolicy, handler, props);
        } catch (AuthException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unit test method.
     */
    protected ServerAuthModule getAuthModule() {
        return new AuthModule();
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        module.cleanSubject(messageInfo, subject);
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return module.secureResponse(messageInfo, serviceSubject);
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
        return module.validateRequest(messageInfo, clientSubject, serviceSubject);
    }

    private MessagePolicy newMessagePolicy(boolean isMandatory, ProtectionPolicy policy) {
        TargetPolicy[] policies = new TargetPolicy[] { new TargetPolicy((Target[]) null, policy) };
        return new MessagePolicy(policies, isMandatory);
    }

    private ProtectionPolicy newProtectionPolicy(final String id) {
        return new ProtectionPolicy() {

            @Override
            public String getID() {
                return id;
            }
        };
    }

}
