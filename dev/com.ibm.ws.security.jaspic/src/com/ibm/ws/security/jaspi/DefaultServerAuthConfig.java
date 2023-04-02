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

package com.ibm.ws.security.jaspi;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 * This class is used to obtain ServerAuthContext objects suitable for processing a given message exchange at the layer and
 * within the application context of the ServerAuthConfig. Each ServerAuthContext object is responsible for instantiating, 
 * initializing, and invoking the one or more ServerAuthModules encapsulated in the ServerAuthContext. 
 */
public class DefaultServerAuthConfig implements ServerAuthConfig {

    private final String layer;
    private final String appContext;
    private final CallbackHandler handler;
    private final Map<String, String> providerProperties;
    private final ServerAuthModule serverAuthModule;

    public DefaultServerAuthConfig(String layer, String appContext, CallbackHandler handler,
                                   Map<String, String> providerProperties, ServerAuthModule serverAuthModule) {
        this.layer = layer;
        this.appContext = appContext;
        this.handler = handler;
        this.providerProperties = providerProperties;
        this.serverAuthModule = serverAuthModule;
    }

    @Override
    public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject,
                                            @SuppressWarnings("rawtypes") Map properties) throws AuthException {
        return new DefaultServerAuthContext(handler, serverAuthModule);
    }

    @Override
    public String getMessageLayer() {
        return layer;
    }

    @Override
    public String getAuthContextID(MessageInfo messageInfo) {
        return appContext;
    }

    @Override
    public String getAppContext() {
        return appContext;
    }

    @Override
    public void refresh() {}

    @Override
    public boolean isProtected() {
        return false;
    }

    public Map<String, String> getProviderProperties() {
        return providerProperties;
    }

}