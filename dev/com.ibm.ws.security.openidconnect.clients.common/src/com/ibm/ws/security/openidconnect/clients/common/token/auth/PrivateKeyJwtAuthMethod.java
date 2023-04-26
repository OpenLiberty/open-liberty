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

import java.security.PrivateKey;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.ssl.KeyStoreService;

import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public class PrivateKeyJwtAuthMethod extends TokenEndpointAuthMethod {

    public static final TraceComponent tc = Tr.register(PrivateKeyJwtAuthMethod.class);

    public static final String AUTH_METHOD = "private_key_jwt";

    private final ConvergedClientConfig clientConfig;

    public PrivateKeyJwtAuthMethod(ConvergedClientConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Sensitive
    public static PrivateKey getPrivateKeyForClientAuthentication(String clientId, String keyAliasName, String keyStoreRef, KeyStoreService keyStoreService) throws Exception {
        if (keyAliasName == null || keyAliasName.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "PRIVATE_KEY_JWT_MISSING_KEY_ALIAS_NAME", clientId);
            throw new Exception(errorMsg);
        }
        if (keyStoreRef == null || keyStoreRef.isEmpty()) {
            String errorMsg = Tr.formatMessage(tc, "PRIVATE_KEY_JWT_MISSING_KEYSTORE_REF", clientId);
            throw new Exception(errorMsg);
        }
        return keyStoreService.getPrivateKeyFromKeyStore(keyStoreRef, keyAliasName, null);
    }

    @Override
    public void setAuthMethodSpecificSettings(Builder tokenRequestBuilder) throws TokenEndpointAuthMethodSettingsException {
        try {
            tokenRequestBuilder.clientAssertionSigningAlgorithm(clientConfig.getTokenEndpointAuthSigningAlgorithm());
            tokenRequestBuilder.clientAssertionSigningKey(clientConfig.getPrivateKeyForClientAuthentication());
        } catch (Exception e) {
            throw new TokenEndpointAuthMethodSettingsException(clientConfig.getClientId(), clientConfig.getTokenEndpointAuthMethod(), e.getMessage());
        }
    }

}
