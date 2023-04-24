/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.common.token.auth;

import java.security.Key;

import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;

import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public class PrivateKeyJwtAuthMethod extends TokenEndpointAuthMethod {

    public static final String AUTH_METHOD = "private_key_jwt";

    private final ConvergedClientConfig clientConfig;

    public PrivateKeyJwtAuthMethod(ConvergedClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    public void setAuthMethodSpecificSettings(Builder tokenRequestBuilder) {
        tokenRequestBuilder.clientAssertionSigningAlgorithm(clientConfig.getTokenEndpointAuthSigningAlgorithm());
        Key clientAssertionSigningKey = getKeyForPrivateKeyJwtClientAssertion();
        tokenRequestBuilder.clientAssertionSigningKey(clientAssertionSigningKey);
    }

    private Key getKeyForPrivateKeyJwtClientAssertion() {
        // TODO
        return null;
    }

}
