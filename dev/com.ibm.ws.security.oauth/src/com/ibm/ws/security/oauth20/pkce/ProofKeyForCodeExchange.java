/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.oauth20.pkce;

import java.security.SecureRandom;
import java.util.Base64;

import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.internal.oauth20.TraceConstants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Class to contain Proof Key for Code Exchange values consistent with RFC 7636.
 */
public class ProofKeyForCodeExchange {

    private static final TraceComponent tc = Tr.register(ProofKeyForCodeExchange.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public static final int CODE_VERIFIER_MIN_LENGTH = 43;
    public static final int CODE_VERIFIER_MAX_LENGTH = 128;

    /**
     * Generates a code verifier value per Section 4.1 of RFC 7636: It is RECOMMENDED that the output of a suitable random number
     * generator be used to create a 32-octet sequence. The octet sequence is then base64url-encoded to produce a 43-octet URL
     * safe string to use as the code verifier. Per Section 3 of RFC 7636, all trailing '=' characters are omitted.
     */
    public static String generateCodeVerifier() {
        SecureRandom random = new SecureRandom();
        byte verifierBytes[] = new byte[32];
        random.nextBytes(verifierBytes);
        String codeVerifier = new String(Base64.getUrlEncoder().encode(verifierBytes));
        codeVerifier = codeVerifier.replaceAll("[=]+$", "");
        return codeVerifier;
    }

    /**
     * Verifies the provided code challenge based on the verifier and challenge method provided.
     */
    public static void verifyCodeChallenge(String codeVerifier, String codeChallenge, String codeChallengeMethod) throws OAuth20MissingParameterException, InvalidGrantException {
        if (codeVerifier == null) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", "code_verifier", null);
        }
        if (!isCodeVerifierLengthAcceptable(codeVerifier)) {
            String message = Tr.formatMessage(tc, "security.oauth20.pkce.codeverifier.length.error", codeVerifier.length());
            throw new InvalidGrantException(message, null);
        }
        ProofKeyForCodeExchangeMethod pkceMethod = ProofKeyForCodeExchangeMethod.getInstance(codeChallengeMethod);
        if (pkceMethod == null) {
            String message = Tr.formatMessage(tc, "security.oauth20.pkce.invalid.method.error", codeChallengeMethod);
            throw new InvalidGrantException(message, null);
        }
        pkceMethod.validate(codeVerifier, codeChallenge);
    }

    static boolean isCodeVerifierLengthAcceptable(String codeVerifier) {
        if (codeVerifier != null && (codeVerifier.length() >= CODE_VERIFIER_MIN_LENGTH && codeVerifier.length() <= CODE_VERIFIER_MAX_LENGTH)) {
            return true;
        }
        return false;
    }

}
