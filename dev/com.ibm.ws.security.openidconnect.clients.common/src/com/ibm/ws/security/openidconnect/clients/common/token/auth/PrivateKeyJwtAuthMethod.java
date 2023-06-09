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

import java.security.Key;
import java.security.PrivateKey;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.security.openidconnect.clients.common.ConvergedClientConfig;
import com.ibm.ws.ssl.KeyStoreService;

import io.openliberty.security.oidcclientcore.exceptions.PrivateKeyJwtAuthException;
import io.openliberty.security.oidcclientcore.exceptions.TokenRequestException;
import io.openliberty.security.oidcclientcore.token.TokenConstants;
import io.openliberty.security.oidcclientcore.token.TokenRequestor.Builder;

public class PrivateKeyJwtAuthMethod extends TokenEndpointAuthMethod {

    public static final TraceComponent tc = Tr.register(PrivateKeyJwtAuthMethod.class);

    public static final String AUTH_METHOD = "private_key_jwt";

    private static boolean issuedBetaMessage = false;

    private final String clientId;
    private final String tokenEndpoint;
    private final String clientAssertionSigningAlgorithm;
    @Sensitive
    private final Key clientAssertionSigningKey;
    private final ConvergedClientConfig clientConfig;

    public PrivateKeyJwtAuthMethod(String clientId, String tokenEndpointAutSigningAlg, String keyAliasName, String keyStoreRef, KeyStoreService keyStoreService) {
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
        this.clientAssertionSigningAlgorithm = clientAssertionSigningAlgorithm;
        this.clientAssertionSigningKey = clientAssertionSigningKey;
        this.clientConfig = clientConfig;
    }

    @Sensitive
    public static PrivateKey getPrivateKeyForClientAuthentication() throws Exception {
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

    void addPrivateKeyJwtParameters(List<NameValuePair> params) throws TokenRequestException {
        if (!isRunningBetaMode()) {
            return;
        }
        params.add(new BasicNameValuePair(TokenConstants.CLIENT_ASSERTION_TYPE, TokenConstants.CLIENT_ASSERTION_TYPE_JWT_BEARER));
        String clientAssertionJwt = buildClientAssertionJwt();
        params.add(new BasicNameValuePair(TokenConstants.CLIENT_ASSERTION, clientAssertionJwt));
    }

    private boolean isRunningBetaMode() {
        if (!ProductInfo.getBetaEdition()) {
            return false;
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
            return true;
        }
    }

    private String buildClientAssertionJwt() throws TokenRequestException {
        PrivateKeyJwtAuthMethod pkjAuthMethod = new PrivateKeyJwtAuthMethod(clientId, tokenEndpoint, clientAssertionSigningAlgorithm, clientAssertionSigningKey);
        try {
            return pkjAuthMethod.createPrivateKeyJwt();
        } catch (PrivateKeyJwtAuthException e) {
            throw new TokenRequestException(clientId, e.getMessage(), e);
        }
    }

}
