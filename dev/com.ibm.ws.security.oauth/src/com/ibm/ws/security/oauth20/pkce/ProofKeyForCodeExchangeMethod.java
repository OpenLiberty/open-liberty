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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ibm.oauth.core.api.error.oauth20.InvalidGrantException;

public abstract class ProofKeyForCodeExchangeMethod {

    public static final Set<String> SUPPORTED_CODE_CHALLENGE_METHODS;
    static {
        Set<String> supportedCodeChallengeMethods = new HashSet<>();
        supportedCodeChallengeMethods.add("plain");
        supportedCodeChallengeMethods.add("S256");
        SUPPORTED_CODE_CHALLENGE_METHODS = Collections.unmodifiableSet(supportedCodeChallengeMethods);
    };

    public static ProofKeyForCodeExchangeMethod getInstance(String challengeMethod) {
        if (challengeMethod == null || challengeMethod.isEmpty()) {
            return null;
        }
        if (PkceMethodS256.CHALLENGE_METHOD.equals(challengeMethod)) {
            return new PkceMethodS256();
        }
        if (PkceMethodPlain.CHALLENGE_METHOD.equals(challengeMethod)) {
            return new PkceMethodPlain();
        }
        return null;
    }

    public static boolean isValidCodeChallengeMethod(String codeChallengeMethod) {
        if (codeChallengeMethod == null) {
            return false;
        }
        return SUPPORTED_CODE_CHALLENGE_METHODS.contains(codeChallengeMethod);
    }

    public abstract String getCodeChallengeMethod();

    public abstract String generateCodeChallenge(String codeVerifier);

    public abstract void validate(String codeVerifier, String codeChallenge) throws InvalidGrantException;

}
