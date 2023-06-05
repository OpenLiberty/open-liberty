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

import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Class to use for generating and validating PKCE code challenges using the "plain" code challenge method.
 */
public class PkceMethodPlain extends ProofKeyForCodeExchangeMethod {

    private static final TraceComponent tc = Tr.register(PkceMethodS256.class);

    public static String CHALLENGE_METHOD = "plain";

    @Override
    public String getCodeChallengeMethod() {
        return CHALLENGE_METHOD;
    }

    /**
     * Note: Plain performs no operations on the code verifier and simply returns the value as-is.
     */
    @Override
    public String generateCodeChallenge(String codeVerifier) {
        return codeVerifier;
    }

    @Override
    public void validate(String codeVerifier, String codeChallenge) throws InvalidGrantException {
        if (codeVerifier == null && codeChallenge == null) {
            return;
        }
        if ((codeChallenge == null && codeVerifier != null) || !codeChallenge.equals(codeVerifier)) {
            String message = Tr.formatMessage(tc, "security.oauth20.pkce.error.mismatch.codeverifier", codeChallenge, codeVerifier);
            throw new InvalidGrantException(message, null);
        }
    }

}
