/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.oauth20.pkce;

import java.nio.charset.StandardCharsets;

import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.HashUtils;

/**
 * Class to use for generating and validating PKCE code challenges using the "S256" code challenge method.
 */
public class PkceMethodS256 extends ProofKeyForCodeExchangeMethod {

    private static final TraceComponent tc = Tr.register(PkceMethodS256.class);

    public static String CHALLENGE_METHOD = "S256";
    public static String CODE_CHALLENGE_ALG_METHOD = "SHA-256";

    @Override
    public String getCodeChallengeMethod() {
        return CHALLENGE_METHOD;
    }

    @Override
    public String generateCodeChallenge(String codeVerifier) {
        return HashUtils.encodedDigest(codeVerifier, CODE_CHALLENGE_ALG_METHOD, StandardCharsets.US_ASCII);
    }

    @Override
    public void validate(String codeVerifier, String codeChallenge) throws InvalidGrantException {
        if (codeVerifier == null && codeChallenge == null) {
            return;
        }
        String derivedCodeChallenge = generateCodeChallenge(codeVerifier);
        if ((codeChallenge == null && codeVerifier != null) || !codeChallenge.equals(derivedCodeChallenge)) {
            String message = Tr.formatMessage(tc, "security.oauth20.pkce.error.mismatch.codeverifier", codeChallenge, codeVerifier);
            throw new InvalidGrantException(message, null);
        }
    }

}
