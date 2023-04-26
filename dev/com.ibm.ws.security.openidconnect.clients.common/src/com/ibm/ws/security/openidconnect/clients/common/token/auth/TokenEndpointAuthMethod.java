/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common.token.auth;

import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;

import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public abstract class TokenEndpointAuthMethod {

    public static TokenEndpointAuthMethod getInstance(String authMethod, ConvergedClientConfig clientConfig) {
        if (authMethod == null || authMethod.isEmpty()) {
            return null;
        }
        if (PrivateKeyJwtAuthMethod.AUTH_METHOD.equals(authMethod)) {
            return new PrivateKeyJwtAuthMethod(clientConfig);
        }
        return null;
    }

    public abstract void setAuthMethodSpecificSettings(Builder tokenRequestBuilder);

}
