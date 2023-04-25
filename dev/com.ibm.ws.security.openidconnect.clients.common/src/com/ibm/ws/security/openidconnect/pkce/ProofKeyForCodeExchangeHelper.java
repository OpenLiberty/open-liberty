/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.pkce;

import java.util.HashMap;

import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.pkce.ProofKeyForCodeExchange;
import com.ibm.ws.security.oauth20.pkce.ProofKeyForCodeExchangeMethod;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;

public class ProofKeyForCodeExchangeHelper {

    private static final TraceComponent tc = Tr.register(ProofKeyForCodeExchangeHelper.class);

    /**
     * Generates a code verifier value, caches that value to be retrieved later for verification, calculates the code challenge
     * based on the provided code challenge method, and adds the code challenge and code challenge method to the parameters that
     * are provided.
     */
    public void generateAndAddPkceParametersToAuthzRequest(String codeChallengeMethod, String state, AuthorizationRequestParameters parameters) {
        ProofKeyForCodeExchangeMethod pkceMethod = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        if (pkceMethod == null) {
            // Shouldn't happen since the code challenge method is coming from the server config
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Did not find a PKCE method class for challenge method [" + codeChallengeMethod + "]");
            }
            return;
        }
        String codeVerifier = getAndCacheCodeVerifier(state);
        String codeChallenge = pkceMethod.generateCodeChallenge(codeVerifier);
        parameters.addParameter(OAuth20Constants.CODE_CHALLENGE, codeChallenge);
        parameters.addParameter(OAuth20Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
    }

    public String getAndCacheCodeVerifier(String state) {
        String codeVerifier = ProofKeyForCodeExchange.generateCodeVerifier();
        ProofKeyForCodeExchangeCache.cacheCodeVerifier(state, codeVerifier);
        return codeVerifier;
    }

    public HashMap<String, String> addCodeVerifierToTokenRequestParameters(String state, HashMap<String, String> parameters) {
        String codeVerifier = ProofKeyForCodeExchangeCache.getCachedCodeVerifier(state);
        parameters.put(OAuth20Constants.CODE_VERIFIER, codeVerifier);
        return parameters;
    }

}
